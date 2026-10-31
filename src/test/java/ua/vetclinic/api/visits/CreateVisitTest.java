package ua.vetclinic.api.visits;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
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
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestData.CLINIC_OFFSET_TIMESTAMP;
import static ua.vetclinic.api.TestData.UUID_PATTERN;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.createVisit;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.postVisit;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

/** REQ-F-027, REQ-F-032, REQ-F-033, AD-07, AD-14: збереження результатів прийому (операція createVisit). */
@ApiIntegrationTest
class CreateVisitTest {

	private static final String SLOT_DATE = "2026-11-03";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Doctor doctor;

	private String patientId;

	private String appointmentId;

	@BeforeEach
	void scheduleAppointment() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Прийому Перший");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Прийому");
		appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot(SLOT_DATE, "10:00"));
	}

	@Test
	@DisplayName("REQ-F-032, REQ-F-033: лікар зберігає прийом — 201, id, appointmentId, patientId, doctorId і результати")
	void assignedDoctorCreatesVisit_returns201() throws Exception {
		// Arrange
		String requestBody = """
				{ "diagnosis": "Бактеріальна інфекція", "prescriptions": "Амоксицилін, курс 5 днів",
				  "notes": "Повторний огляд через тиждень" }
				""";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.appointmentId", equalTo(appointmentId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.doctorId", equalTo(doctor.id())))
				.andExpect(jsonPath("$.diagnosis", equalTo("Бактеріальна інфекція")))
				.andExpect(jsonPath("$.prescriptions", equalTo("Амоксицилін, курс 5 днів")))
				.andExpect(jsonPath("$.notes", equalTo("Повторний огляд через тиждень")));
	}

	@Test
	@DisplayName("Location: заголовок вказує на створений прийом /api/v1/visits/{id}")
	void createVisit_returnsLocationOfVisit() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\" }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/visits/" + UUID_PATTERN)));
	}

	@Test
	@DisplayName("AD-14: visitDate дорівнює startsAt запису, а не моменту збереження")
	void visitDate_equalsAppointmentStart() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\" }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.visitDate", equalTo(slot(SLOT_DATE, "10:00"))));
	}

	@Test
	@DisplayName("AD-19.1: createdAt і updatedAt — ISO 8601 із зсувом часового поясу клініки")
	void timestamps_areClinicOffset() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\" }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.visitDate", matchesPattern(CLINIC_OFFSET_TIMESTAMP)));
	}

	@Test
	@DisplayName("AD-12: обов'язковий лише діагноз — без призначень, нотаток і медикаментів прийом зберігається")
	void onlyDiagnosis_isEnough() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Лише діагноз\" }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.prescriptions", nullValue()))
				.andExpect(jsonPath("$.notes", nullValue()))
				.andExpect(jsonPath("$.medications", empty()));
	}

	@Test
	@DisplayName("AD-17.1: prescriptions і notes явно null приймаються як «не вказано»")
	void nullPrescriptionsAndNotes_areAccepted() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"prescriptions\": null, \"notes\": null }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.prescriptions", nullValue()))
				.andExpect(jsonPath("$.notes", nullValue()));
	}

	@Test
	@DisplayName("AD-07.3: medications явно null і порожній масив рівнозначні — прийом без препаратів")
	void nullMedications_areTreatedAsEmpty() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": null }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.medications", empty()));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях діагнозу, призначень і нотаток обрізаються")
	void textFields_areTrimmed() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"  Діагноз  \", \"prescriptions\": \" Призначення \", \"notes\": \" Нотатки \" }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.diagnosis", equalTo("Діагноз")))
				.andExpect(jsonPath("$.prescriptions", equalTo("Призначення")))
				.andExpect(jsonPath("$.notes", equalTo("Нотатки")));
	}

	@Test
	@DisplayName("REQ-F-044: у відповіді перелік витрачених препаратів — medicationId, medicationName і quantity")
	void medications_areReturnedWithNames() throws Exception {
		// Arrange
		String name = uniqueName("Амоксицилін прийом");
		String medicationId = createMedication(mockMvc, name, 10, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\", \"quantity\": 3 } ] }";

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.medications", hasSize(1)))
				.andExpect(jsonPath("$.medications[0].medicationId", equalTo(medicationId)))
				.andExpect(jsonPath("$.medications[0].medicationName", equalTo(name)))
				.andExpect(jsonPath("$.medications[0].quantity", equalTo(3)));
	}

	@Test
	@DisplayName("REQ-F-027: після збереження прийому запис отримує статус COMPLETED і посилання visitId")
	void createVisit_completesAppointment() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("COMPLETED")))
				.andExpect(jsonPath("$.visitId", equalTo(visitId)));
	}

	@Test
	@DisplayName("AD-03.1: слот завершеного запису лишається зайнятим — новий запис на нього дає 409 SLOT_TAKEN")
	void completedAppointment_keepsSlotOccupied() throws Exception {
		// Arrange
		createVisit(mockMvc, doctor, appointmentId);
		String otherPatientId = createPatientWithOwner(mockMvc, "Пацієнт Зайнятого Слота");

		// Act
		var response = mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"doctorId\": \"" + doctor.id() + "\", \"patientId\": \"" + otherPatientId
						+ "\", \"startsAt\": \"" + slot(SLOT_DATE, "10:00") + "\" }"));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("SLOT_TAKEN")));
	}

	@Test
	@DisplayName("AD-03.2: завершений запис не можна скасувати — 409 APPOINTMENT_NOT_SCHEDULED")
	void completedAppointment_cannotBeCancelled() throws Exception {
		// Arrange
		createVisit(mockMvc, doctor, appointmentId);

		// Act
		var response = mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/cancel")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("APPOINTMENT_NOT_SCHEDULED")));
	}

	@Test
	@DisplayName("AD-07.1: другий прийом для того самого запису — 409 APPOINTMENT_NOT_SCHEDULED")
	void secondVisitForAppointment_returns409() throws Exception {
		// Arrange
		createVisit(mockMvc, doctor, appointmentId);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Повторно\" }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("APPOINTMENT_NOT_SCHEDULED")));
	}

	@Test
	@DisplayName("AD-07.1: прийом для скасованого запису — 409 APPOINTMENT_NOT_SCHEDULED")
	void cancelledAppointment_returns409() throws Exception {
		// Arrange
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Діагноз\" }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("APPOINTMENT_NOT_SCHEDULED")));
	}

	@Test
	@DisplayName("AD-07.1: прийом, збережений іншим лікарем — 403 NOT_ASSIGNED_DOCTOR, запис лишається SCHEDULED")
	void otherDoctor_returns403() throws Exception {
		// Arrange
		Doctor otherDoctor = newDoctor(mockMvc, "Лікар Прийому Інший");

		// Act
		var response = postVisit(mockMvc, otherDoctor, appointmentId, "{ \"diagnosis\": \"Чужий запис\" }");

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("NOT_ASSIGNED_DOCTOR")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-18.4: чужий лікар на скасованому записі отримує 403 NOT_ASSIGNED_DOCTOR раніше за 409")
	void otherDoctorOnCancelledAppointment_returns403BeforeConflict() throws Exception {
		// Arrange
		Doctor otherDoctor = newDoctor(mockMvc, "Лікар Прийому Сторонній");
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = postVisit(mockMvc, otherDoctor, appointmentId, "{ \"diagnosis\": \"Діагноз\" }");

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("NOT_ASSIGNED_DOCTOR")));
	}

	@Test
	@DisplayName("AD-04.1, REQ-F-035: пацієнт із архівною карткою — 409 PATIENT_ARCHIVED, запис лишається SCHEDULED")
	void archivedPatient_returns409() throws Exception {
		// Arrange
		jdbcTemplate.update("update patients set status = 'ARCHIVED' where id = ?::uuid", patientId);

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Діагноз\" }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_ARCHIVED")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-18.2: запису з таким id немає — 404 NOT_FOUND")
	void unknownAppointment_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000b01";

		// Act
		var response = postVisit(mockMvc, doctor, unknownId, "{ \"diagnosis\": \"Діагноз\" }");

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.2: id запису не UUID — 400 MALFORMED_REQUEST")
	void malformedAppointmentId_returns400() throws Exception {
		// Arrange
		String malformedId = "не-uuid";

		// Act
		var response = postVisit(mockMvc, doctor, malformedId, "{ \"diagnosis\": \"Діагноз\" }");

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — некоректне тіло для неіснуючого запису дає 422, а не 404")
	void invalidBodyForUnknownAppointment_returns422() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000b02";

		// Act
		var response = postVisit(mockMvc, doctor, unknownId, "{}");

		// Assert
		response.andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("REQ-F-031: адміністратор не може зберігати прийом — 403 FORBIDDEN, запис лишається SCHEDULED")
	void admin_returns403() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments/" + appointmentId + "/visit")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Діагноз\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("REQ-NF-002: POST /appointments/{id}/visit без токена — 401 UNAUTHORIZED")
	void withoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments/" + appointmentId + "/visit")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Діагноз\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-18.2: тіло не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void textBody_returns415() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments/" + appointmentId + "/visit")
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.TEXT_PLAIN)
				.content("diagnosis=Текст");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}

	@Test
	@DisplayName("AD-07.1: паралельні прийоми для одного запису — рівно один 201 і решта 409 APPOINTMENT_NOT_SCHEDULED")
	void concurrentVisitsForOneAppointment_createExactlyOne() throws Exception {
		// Arrange
		var executor = Executors.newFixedThreadPool(6);
		List<Callable<Integer>> calls = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			calls.add(() -> postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Паралельно\" }").andReturn()
					.getResponse().getStatus());
		}

		// Act
		var results = executor.invokeAll(calls);
		executor.shutdown();

		// Assert
		int created = 0;
		int conflicts = 0;
		for (var result : results) {
			created += result.get() == 201 ? 1 : 0;
			conflicts += result.get() == 409 ? 1 : 0;
		}
		assertThat(created, equalTo(1));
		assertThat(conflicts, equalTo(5));
	}

	@Test
	@DisplayName("AD-09: DELETE /visits/{id} — 405 METHOD_NOT_ALLOWED, медичні записи не видаляються")
	void deleteVisit_returns405() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);

		// Act
		var response = mockMvc.perform(delete("/api/v1/visits/" + visitId).header("Authorization", doctor.bearer()));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("GET")));
	}
}
