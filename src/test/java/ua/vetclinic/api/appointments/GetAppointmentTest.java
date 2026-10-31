package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-021: перегляд запису на прийом (операція getAppointment); читання доступне адміністратору й лікарю. */
@ApiIntegrationTest
class GetAppointmentTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-021: адміністратор отримує запис за id — 200 з усіма полями")
	void adminGetsAppointment_returnsAllFields() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Перегляд Запису Лікар");
		String patientId = createPatientWithOwner(mockMvc, "Перегляд Запису Пацієнт");
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-18", "13:30"));

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-18", "13:30"))))
				.andExpect(jsonPath("$.endsAt", equalTo(slot("2026-12-18", "14:00"))))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("REQ-F-026: лікар може переглядати запис — 200")
	void veterinarianGetsAppointment_returns200() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Перегляд Лікарем Лікар");
		String patientId = createPatientWithOwner(mockMvc, "Перегляд Лікарем Пацієнт");
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-18", "14:30"));

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(appointmentId)));
	}

	@Test
	@DisplayName("REQ-F-021: неіснуючий запис — 404 NOT_FOUND")
	void unknownAppointment_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000641";

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments/" + unknownId).with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор запису не UUID — 400 MALFORMED_REQUEST")
	void appointmentIdNotUuid_returns400() throws Exception {
		// Arrange
		var request = get("/api/v1/appointments/not-a-uuid").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /appointments/{id} без токена — 401 UNAUTHORIZED")
	void getWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/appointments/00000000-0000-4000-8000-000000000641");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}
}
