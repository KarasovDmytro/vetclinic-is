package ua.vetclinic.api.appointments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestData.CLINIC_OFFSET_TIMESTAMP;
import static ua.vetclinic.api.TestData.UUID_PATTERN;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-021, REQ-F-022, REQ-F-023, AD-03, AD-04, AD-05: створення запису на прийом (операція createAppointment). */
@ApiIntegrationTest
class CreateAppointmentTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postAppointment(String doctorId, String patientId, String startsAt) throws Exception {
		return mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s" }
						""".formatted(doctorId, patientId, startsAt)));
	}

	@Test
	@DisplayName("REQ-F-021, REQ-F-022: адміністратор створює запис — 201, Location, статус SCHEDULED від сервера, endsAt = +30 хв")
	void adminCreatesAppointment_returns201WithScheduledStatus() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Перший");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Перший");

		// Act
		var response = postAppointment(doctorId, patientId, slot("2026-12-01", "14:00"));

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/appointments/" + UUID_PATTERN)))
				.andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-01", "14:00"))))
				.andExpect(jsonPath("$.endsAt", equalTo(slot("2026-12-01", "14:30"))))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.visitId", nullValue()))
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)));
	}

	@Test
	@DisplayName("AD-05.2: час в UTC нормалізується до часового поясу клініки — 08:00Z це слот 10:00+02:00")
	void startsAtInUtc_isNormalizedToClinicZone() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар UTC");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт UTC");

		// Act
		var response = postAppointment(doctorId, patientId, "2026-12-01T08:00:00Z");

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-01", "10:00"))));
	}

	@Test
	@DisplayName("REQ-F-023: слот лікаря вже зайнятий — 409 SLOT_TAKEN, другий запис не зберігається")
	void takenSlot_returns409SlotTaken() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Конфлікт");
		String firstPatient = createPatientWithOwner(mockMvc, "Запис Пацієнт А");
		String secondPatient = createPatientWithOwner(mockMvc, "Запис Пацієнт Б");
		String firstId = createAppointment(mockMvc, doctorId, firstPatient, slot("2026-12-02", "10:00"));

		// Act
		var response = postAppointment(doctorId, secondPatient, slot("2026-12-02", "10:00"));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("SLOT_TAKEN")))
				.andExpect(jsonPath("$.message", equalTo("Слот вже зайнятий")))
				.andExpect(jsonPath("$.status", equalTo(409)))
				.andExpect(jsonPath("$.path", equalTo("/api/v1/appointments")));
		mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", "2026-12-02").param("to", "2026-12-02").with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.slots[2].appointment.id", equalTo(firstId)))
				.andExpect(jsonPath("$.slots[?(@.status == 'BOOKED')]", hasSize(1)));
	}

	@Test
	@DisplayName("REQ-F-023: той самий слот для іншого лікаря створюється без конфлікту")
	void sameSlotForOtherDoctor_returns201() throws Exception {
		// Arrange
		String firstDoctor = createDoctor(mockMvc, "Запис Лікар Один");
		String secondDoctor = createDoctor(mockMvc, "Запис Лікар Два");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Два Лікарі");
		createAppointment(mockMvc, firstDoctor, patientId, slot("2026-12-03", "10:00"));

		// Act
		var response = postAppointment(secondDoctor, patientId, slot("2026-12-03", "10:00"));

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("REQ-F-023: сусідній слот того самого лікаря створюється без конфлікту")
	void adjacentSlotOfSameDoctor_returns201() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Сусідній");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Сусідній");
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-03", "10:00"));

		// Act
		var before = postAppointment(doctorId, patientId, slot("2026-12-03", "09:30"));
		var after = postAppointment(doctorId, patientId, slot("2026-12-03", "10:30"));

		// Assert
		before.andExpect(status().isCreated());
		after.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-03.1: скасований запис звільняє слот — на нього можна створити новий запис")
	void slotOfCancelledAppointment_canBeBookedAgain() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Повторно");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Повторно");
		String firstId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-04", "10:00"));
		cancelAppointment(mockMvc, firstId);

		// Act
		var response = postAppointment(doctorId, patientId, slot("2026-12-04", "10:00"));

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("REQ-F-023: паралельні запити на один слот — рівно один 201, решта 409 SLOT_TAKEN")
	void concurrentRequestsForSameSlot_createExactlyOneAppointment() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Гонка");
		java.util.List<String> patients = new java.util.ArrayList<>();
		for (int i = 0; i < 8; i++) {
			patients.add(createPatientWithOwner(mockMvc, "Запис Пацієнт Гонка " + i));
		}
		String startsAt = slot("2026-12-09", "10:00");
		var executor = java.util.concurrent.Executors.newFixedThreadPool(8);
		var start = new java.util.concurrent.CountDownLatch(1);

		// Act
		var futures = patients.stream().map(patientId -> executor.submit(() -> {
			start.await();
			return mockMvc.perform(post("/api/v1/appointments")
					.with(user("admin").roles("ADMIN"))
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s" }
							""".formatted(doctorId, patientId, startsAt)))
					.andReturn().getResponse().getStatus();
		})).toList();
		start.countDown();
		java.util.List<Integer> statuses = new java.util.ArrayList<>();
		for (var future : futures) {
			statuses.add(future.get());
		}
		executor.shutdown();

		// Assert
		assertThat(statuses.stream().filter(code -> code == 201).count(), equalTo(1L));
		assertThat(statuses.stream().filter(code -> code == 409).count(), equalTo(7L));
	}

	@Test
	@DisplayName("AD-04.1: запис для архівного пацієнта — 409 PATIENT_ARCHIVED")
	void archivedPatient_returns409PatientArchived() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Архів");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Архівний");
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = postAppointment(doctorId, patientId, slot("2026-12-10", "10:00"));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_ARCHIVED")))
				.andExpect(jsonPath("$.message", equalTo("Картку пацієнта архівовано")));
	}

	@Test
	@DisplayName("AD-18.3: неіснуючий пацієнт — 422, поле patientId, причина REFERENCE_NOT_FOUND (не 404)")
	void unknownPatient_returns422ReferenceNotFound() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Без Пацієнта");
		String unknownPatient = "00000000-0000-4000-8000-000000000611";

		// Act
		var response = postAppointment(doctorId, unknownPatient, slot("2026-12-10", "11:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("patientId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REFERENCE_NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.3: неіснуючий лікар — 422, поле doctorId, причина REFERENCE_NOT_FOUND")
	void unknownDoctor_returns422ReferenceNotFound() throws Exception {
		// Arrange
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Без Лікаря");
		String unknownDoctor = "00000000-0000-4000-8000-000000000612";

		// Act
		var response = postAppointment(unknownDoctor, patientId, slot("2026-12-10", "11:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REFERENCE_NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.3: користувач з роллю ADMIN не може бути лікарем запису — 422 REFERENCE_NOT_FOUND")
	void administratorAsDoctor_returns422ReferenceNotFound() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "appointments.admin." + UUID.randomUUID() + "@clinic.example",
				"Запис Адмін Не Лікар", "ADMIN");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Адмін");

		// Act
		var response = postAppointment(adminId, patientId, slot("2026-12-10", "11:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REFERENCE_NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-20.1: деактивований лікар — 422, поле doctorId, причина DOCTOR_NOT_AVAILABLE")
	void deactivatedDoctor_returns422DoctorNotAvailable() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Деактивований");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Деактивований Лікар");
		mockMvc.perform(post("/api/v1/users/" + doctorId + "/deactivate").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = postAppointment(doctorId, patientId, slot("2026-12-10", "11:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("DOCTOR_NOT_AVAILABLE")));
	}

	@Test
	@DisplayName("AD-18: помилки лікаря й пацієнта повертаються разом у details")
	void unknownDoctorAndPatient_areBothReported() throws Exception {
		// Arrange
		String unknownDoctor = "00000000-0000-4000-8000-000000000612";
		String unknownPatient = "00000000-0000-4000-8000-000000000611";

		// Act
		var response = postAppointment(unknownDoctor, unknownPatient, slot("2026-12-10", "11:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(2)));
	}

	@Test
	@DisplayName("AD-17.5: створення запису на минулу дату дозволене (внесення заднім числом)")
	void pastDate_returns201() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Минуле");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Минуле");

		// Act
		var response = postAppointment(doctorId, patientId, slot("2020-01-15", "10:00"));

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-17.1: статус задає лише сервер — поле status у запиті дає 422 UNKNOWN_FIELD")
	void statusInBody_returns422UnknownField() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Статус");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Статус");
		String requestBody = """
				{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s", "status": "COMPLETED" }
				""".formatted(doctorId, patientId, slot("2026-12-07", "10:00"));

		// Act
		var response = mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("status")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: endsAt формує сервер — поле endsAt у запиті дає 422 UNKNOWN_FIELD")
	void endsAtInBody_returns422UnknownField() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Кінець");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Кінець");
		String requestBody = """
				{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s", "endsAt": "%s" }
				""".formatted(doctorId, patientId, slot("2026-12-07", "11:00"), slot("2026-12-07", "11:30"));

		// Act
		var response = mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("endsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може створювати записи — 403 FORBIDDEN")
	void veterinarianCreatesAppointment_returns403() throws Exception {
		// Arrange
		String requestBody = """
				{ "doctorId": "00000000-0000-4000-8000-000000000612",
				  "patientId": "00000000-0000-4000-8000-000000000611", "startsAt": "%s" }
				""".formatted(slot("2026-12-07", "12:00"));

		// Act
		var response = mockMvc.perform(post("/api/v1/appointments")
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-NF-002: POST /appointments без токена — 401 UNAUTHORIZED")
	void createWithoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-F-021: створений запис повертається через GET за адресою з Location")
	void createdAppointment_isReturnedByLocation() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Запис Лікар Location");
		String patientId = createPatientWithOwner(mockMvc, "Запис Пацієнт Location");
		var created = postAppointment(doctorId, patientId, slot("2026-12-08", "10:00")).andReturn();
		String location = created.getResponse().getHeader("Location");
		String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Act
		var response = mockMvc.perform(get(location).with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(id)))
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
		assertThat(location, matchesPattern("/api/v1/appointments/" + UUID_PATTERN));
	}
}
