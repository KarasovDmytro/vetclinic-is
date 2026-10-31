package ua.vetclinic.api.appointments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import java.time.OffsetDateTime;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-023, REQ-F-024, REQ-F-026, AD-03, AD-20.1: перенесення запису на інший слот (операція rescheduleAppointment). */
@ApiIntegrationTest
class RescheduleAppointmentTest {

	private static final String UNKNOWN_ID = "00000000-0000-4000-8000-000000000631";

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	private String patientId;

	private String appointmentId;

	@BeforeEach
	void createAppointmentToMove() throws Exception {
		doctorId = createDoctor(mockMvc, "Перенесення Лікар");
		patientId = createPatientWithOwner(mockMvc, "Перенесення Пацієнт");
		appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-16", "14:00"));
	}

	private ResultActions reschedule(String id, String requestBody) throws Exception {
		return mockMvc.perform(patch("/api/v1/appointments/" + id)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	private String startsAtBody(String time) {
		return "{ \"startsAt\": \"" + slot("2026-12-16", time) + "\" }";
	}

	private ResultActions schedule() throws Exception {
		return mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", "2026-12-16").param("to", "2026-12-16").with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("REQ-F-024: перенесення на вільний слот — 200, id, лікар, пацієнт і статус SCHEDULED лишаються, час новий")
	void rescheduleToFreeSlot_returns200AndKeepsIdentity() throws Exception {
		// Arrange
		String requestBody = startsAtBody("15:00");

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-16", "15:00"))))
				.andExpect(jsonPath("$.endsAt", equalTo(slot("2026-12-16", "15:30"))))
				.andExpect(jsonPath("$.visitId", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-024: старий слот одразу звільняється, новий стає зайнятим тим самим записом")
	void reschedule_freesOldSlotAndBooksNewOne() throws Exception {
		// Arrange
		reschedule(appointmentId, startsAtBody("15:00")).andExpect(status().isOk());

		// Act
		var response = schedule();

		// Assert
		response.andExpect(jsonPath("$.slots[10].startsAt", equalTo(slot("2026-12-16", "14:00"))))
				.andExpect(jsonPath("$.slots[10].status", equalTo("FREE")))
				.andExpect(jsonPath("$.slots[12].startsAt", equalTo(slot("2026-12-16", "15:00"))))
				.andExpect(jsonPath("$.slots[12].status", equalTo("BOOKED")))
				.andExpect(jsonPath("$.slots[12].appointment.id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.slots[?(@.status == 'BOOKED')]", hasSize(1)));
	}

	@Test
	@DisplayName("REQ-F-024: звільнений слот можна одразу зайняти іншим записом")
	void oldSlot_canBeBookedByAnotherAppointment() throws Exception {
		// Arrange
		reschedule(appointmentId, startsAtBody("15:00")).andExpect(status().isOk());
		String otherPatient = createPatientWithOwner(mockMvc, "Перенесення Інший Пацієнт");

		// Act
		var response = mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s" }
						""".formatted(doctorId, otherPatient, slot("2026-12-16", "14:00"))));

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("REQ-F-023: новий слот зайнятий — 409 SLOT_TAKEN, запис лишається на старому слоті")
	void rescheduleToTakenSlot_returns409AndKeepsOldSlot() throws Exception {
		// Arrange
		String otherPatient = createPatientWithOwner(mockMvc, "Перенесення Конфлікт Пацієнт");
		createAppointment(mockMvc, doctorId, otherPatient, slot("2026-12-16", "16:00"));

		// Act
		var response = reschedule(appointmentId, startsAtBody("16:00"));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("SLOT_TAKEN")))
				.andExpect(jsonPath("$.message", equalTo("Слот вже зайнятий")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-16", "14:00"))));
	}

	@Test
	@DisplayName("AD-03.1: слот скасованого запису вільний — перенесення на нього допустиме")
	void rescheduleToSlotOfCancelledAppointment_returns200() throws Exception {
		// Arrange
		String otherPatient = createPatientWithOwner(mockMvc, "Перенесення Скасований Пацієнт");
		String otherId = createAppointment(mockMvc, doctorId, otherPatient, slot("2026-12-16", "16:00"));
		cancelAppointment(mockMvc, otherId);

		// Act
		var response = reschedule(appointmentId, startsAtBody("16:00"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-16", "16:00"))));
	}

	@Test
	@DisplayName("AD-03.2: перенесення скасованого запису — 409 APPOINTMENT_NOT_SCHEDULED")
	void rescheduleCancelledAppointment_returns409() throws Exception {
		// Arrange
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = reschedule(appointmentId, startsAtBody("15:00"));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("APPOINTMENT_NOT_SCHEDULED")));
	}

	@Test
	@DisplayName("AD-20.1: лікаря запису деактивовано — перенесення відхиляється 422 DOCTOR_NOT_AVAILABLE, запис лишається")
	void rescheduleAppointmentOfDeactivatedDoctor_returns422() throws Exception {
		// Arrange
		mockMvc.perform(post("/api/v1/users/" + doctorId + "/deactivate").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = reschedule(appointmentId, startsAtBody("15:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("DOCTOR_NOT_AVAILABLE")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-16", "14:00"))))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-17.5: перенесення на минулу дату дозволене")
	void rescheduleToPastDate_returns200() throws Exception {
		// Arrange
		String requestBody = "{ \"startsAt\": \"" + slot("2020-02-10", "10:00") + "\" }";

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("REQ-F-024: createdAt не змінюється, updatedAt після перенесення збільшується")
	void reschedule_keepsCreatedAtAndMovesUpdatedAt() throws Exception {
		// Arrange
		String before = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN"))).andReturn().getResponse().getContentAsString();

		// Act
		String after = reschedule(appointmentId, startsAtBody("15:00")).andExpect(status().isOk()).andReturn()
				.getResponse().getContentAsString();

		// Assert
		assertThat((String) JsonPath.read(after, "$.createdAt"), equalTo(JsonPath.read(before, "$.createdAt")));
		long updatedBefore = OffsetDateTime.parse((String) JsonPath.read(before, "$.updatedAt")).toInstant()
				.toEpochMilli();
		long updatedAfter = OffsetDateTime.parse((String) JsonPath.read(after, "$.updatedAt")).toInstant()
				.toEpochMilli();
		assertThat(updatedAfter, greaterThan(updatedBefore));
	}

	@Test
	@DisplayName("AD-17.5: startsAt не вказано (порожнє тіло) — 422, поле startsAt, причина REQUIRED")
	void emptyBody_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{}";

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: startsAt явно null — 422, поле startsAt, причина REQUIRED")
	void nullStartsAt_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"startsAt\": null }";

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-05.3: початок не на межі слота — 422, поле startsAt, причина SLOT_NOT_ALIGNED")
	void misalignedStart_returns422() throws Exception {
		// Arrange
		String requestBody = startsAtBody("15:10");

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("SLOT_NOT_ALIGNED")));
	}

	@Test
	@DisplayName("AD-05.3: початок поза робочими годинами — 422, поле startsAt, причина OUTSIDE_WORKING_HOURS")
	void outsideWorkingHours_returns422() throws Exception {
		// Arrange
		String requestBody = startsAtBody("18:00");

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUTSIDE_WORKING_HOURS")));
	}

	@Test
	@DisplayName("AD-18.2: startsAt у неправильному форматі — 400 MALFORMED_REQUEST")
	void invalidTimestamp_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"startsAt\": \"завтра о другій\" }";

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-19.3: лікар, пацієнт і статус змінити не можна — doctorId у тілі дає 422 UNKNOWN_FIELD")
	void doctorIdInBody_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = "{ \"startsAt\": \"" + slot("2026-12-16", "15:00") + "\", \"doctorId\": \"" + doctorId
				+ "\" }";

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-19.4: статус не можна задати полем PATCH — status у тілі дає 422 UNKNOWN_FIELD")
	void statusInBody_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = "{ \"startsAt\": \"" + slot("2026-12-16", "15:00") + "\", \"status\": \"COMPLETED\" }";

		// Act
		var response = reschedule(appointmentId, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("status")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("REQ-F-024: невдале перенесення нічого не змінює — запис лишається на старому слоті")
	void rejectedReschedule_keepsOldSlot() throws Exception {
		// Arrange
		String requestBody = startsAtBody("15:10");

		// Act
		reschedule(appointmentId, requestBody).andExpect(status().isUnprocessableContent());

		// Assert
		schedule().andExpect(jsonPath("$.slots[10].appointment.id", equalTo(appointmentId)));
	}

	@Test
	@DisplayName("REQ-F-024: перенесення неіснуючого запису — 404 NOT_FOUND")
	void rescheduleUnknownAppointment_returns404() throws Exception {
		// Arrange
		String id = UNKNOWN_ID;

		// Act
		var response = reschedule(id, startsAtBody("15:00"));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор запису не UUID — 400 MALFORMED_REQUEST")
	void rescheduleWithInvalidId_returns400() throws Exception {
		// Arrange
		String id = "not-a-uuid";

		// Act
		var response = reschedule(id, startsAtBody("15:00"));

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — невалідний слот для неіснуючого запису дає 422, а не 404")
	void invalidSlotForUnknownAppointment_returns422Before404() throws Exception {
		// Arrange
		String requestBody = startsAtBody("15:10");

		// Act
		var response = reschedule(UNKNOWN_ID, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("REQ-F-026: лікар не може переносити записи — 403 FORBIDDEN, запис лишається на місці")
	void veterinarianReschedules_returns403() throws Exception {
		// Arrange
		var request = patch("/api/v1/appointments/" + appointmentId)
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(startsAtBody("15:00"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		schedule().andExpect(jsonPath("$.slots[10].appointment.id", equalTo(appointmentId)));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — лікар на неіснуючому записі отримує 403, а не 404")
	void veterinarianReschedulesUnknown_returns403Before404() throws Exception {
		// Arrange
		var request = patch("/api/v1/appointments/" + UNKNOWN_ID)
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(startsAtBody("15:00"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-NF-002: PATCH /appointments/{id} без токена — 401 UNAUTHORIZED")
	void rescheduleWithoutToken_returns401() throws Exception {
		// Arrange
		var request = patch("/api/v1/appointments/" + appointmentId)
				.contentType(MediaType.APPLICATION_JSON)
				.content(startsAtBody("15:00"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-19.3: PUT /appointments/{id} — 405 METHOD_NOT_ALLOWED (змінюється лише через PATCH)")
	void putAppointment_returns405() throws Exception {
		// Arrange
		var request = put("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(startsAtBody("15:00"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed());
	}
}
