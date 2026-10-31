package ua.vetclinic.api.appointments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-025, REQ-F-026, AD-03, AD-16, AD-20.1: адміністратор скасовує запис (операція cancelAppointment). */
@ApiIntegrationTest
class CancelAppointmentTest {

	private static final String UNKNOWN_ID = "00000000-0000-4000-8000-000000000621";

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	private String patientId;

	private String appointmentId;

	@BeforeEach
	void createAppointmentToCancel() throws Exception {
		doctorId = createDoctor(mockMvc, "Скасування Лікар");
		patientId = createPatientWithOwner(mockMvc, "Скасування Пацієнт");
		appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-15", "10:00"));
	}

	private ResultActions cancel(String id) throws Exception {
		return mockMvc.perform(post("/api/v1/appointments/" + id + "/cancel").with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("REQ-F-025: адміністратор скасовує запис — 200, статус CANCELLED, решта даних без змін")
	void adminCancelsAppointment_returns200WithCancelledStatus() throws Exception {
		// Arrange
		String id = appointmentId;

		// Act
		var response = cancel(id);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(id)))
				.andExpect(jsonPath("$.status", equalTo("CANCELLED")))
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-15", "10:00"))))
				.andExpect(jsonPath("$.visitId", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-025: запис не видаляється — після скасування GET повертає його зі статусом CANCELLED")
	void cancelledAppointment_isStillReturnedByGet() throws Exception {
		// Arrange
		cancel(appointmentId).andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("CANCELLED")));
	}

	@Test
	@DisplayName("AD-03.1: скасування одразу звільняє слот у сітці розкладу")
	void cancel_freesSlotInSchedule() throws Exception {
		// Arrange
		cancel(appointmentId).andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", "2026-12-15").param("to", "2026-12-15").with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(jsonPath("$.slots[2].status", equalTo("FREE")))
				.andExpect(jsonPath("$.slots[2].appointment", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-025: createdAt не змінюється, updatedAt після скасування збільшується")
	void cancel_keepsCreatedAtAndMovesUpdatedAt() throws Exception {
		// Arrange
		String before = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN"))).andReturn().getResponse().getContentAsString();

		// Act
		String after = cancel(appointmentId).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();

		// Assert
		assertThat((String) JsonPath.read(after, "$.createdAt"), equalTo(JsonPath.read(before, "$.createdAt")));
		long updatedBefore = OffsetDateTime.parse((String) JsonPath.read(before, "$.updatedAt")).toInstant()
				.toEpochMilli();
		long updatedAfter = OffsetDateTime.parse((String) JsonPath.read(after, "$.updatedAt")).toInstant()
				.toEpochMilli();
		assertThat(updatedAfter, greaterThan(updatedBefore));
	}

	@Test
	@DisplayName("AD-16.1: повторне скасування — 409 APPOINTMENT_NOT_SCHEDULED з повною моделлю помилки")
	void cancelTwice_returns409AppointmentNotScheduled() throws Exception {
		// Arrange
		cancel(appointmentId).andExpect(status().isOk());

		// Act
		var response = cancel(appointmentId);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("APPOINTMENT_NOT_SCHEDULED")))
				.andExpect(jsonPath("$.message", equalTo("Запис не у статусі «Заплановано»")))
				.andExpect(jsonPath("$.status", equalTo(409)))
				.andExpect(jsonPath("$.path", equalTo("/api/v1/appointments/" + appointmentId + "/cancel")));
	}

	@Test
	@DisplayName("AD-20.1: запис деактивованого лікаря скасувати можна")
	void appointmentOfDeactivatedDoctor_canBeCancelled() throws Exception {
		// Arrange
		mockMvc.perform(post("/api/v1/users/" + doctorId + "/deactivate").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = cancel(appointmentId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("CANCELLED")));
	}

	@Test
	@DisplayName("REQ-F-025: скасування одного запису не торкається інших записів того самого пацієнта")
	void cancel_doesNotAffectOtherAppointmentsOfPatient() throws Exception {
		// Arrange
		String otherId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-15", "11:00"));

		// Act
		cancel(appointmentId).andExpect(status().isOk());

		// Assert
		mockMvc.perform(get("/api/v1/appointments/" + otherId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("REQ-F-025: скасування неіснуючого запису — 404 NOT_FOUND")
	void cancelUnknownAppointment_returns404() throws Exception {
		// Arrange
		String id = UNKNOWN_ID;

		// Act
		var response = cancel(id);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор запису не UUID — 400 MALFORMED_REQUEST")
	void cancelWithInvalidId_returns400() throws Exception {
		// Arrange
		String id = "not-a-uuid";

		// Act
		var response = cancel(id);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-026: лікар не може скасовувати записи — 403 FORBIDDEN, запис лишається SCHEDULED")
	void veterinarianCancels_returns403() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments/" + appointmentId + "/cancel")
				.with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — лікар на неіснуючому записі отримує 403, а не 404")
	void veterinarianCancelsUnknown_returns403Before404() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments/" + UNKNOWN_ID + "/cancel").with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-NF-002: POST /appointments/{id}/cancel без токена — 401 UNAUTHORIZED")
	void cancelWithoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments/" + appointmentId + "/cancel");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-09: DELETE /appointments/{id} — 405 METHOD_NOT_ALLOWED із Allow, запис не видаляється")
	void deleteAppointment_returns405AndKeepsAppointment() throws Exception {
		// Arrange
		var request = delete("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("GET")))
				.andExpect(header().string("Allow", containsString("PATCH")));
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-19.4: GET /appointments/{id}/cancel — 405 METHOD_NOT_ALLOWED, скасування лише через POST")
	void getCancelEndpoint_returns405() throws Exception {
		// Arrange
		var request = get("/api/v1/appointments/" + appointmentId + "/cancel").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("POST")));
	}
}
