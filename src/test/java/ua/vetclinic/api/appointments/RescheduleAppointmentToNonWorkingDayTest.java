package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-049, AD-25: перенесення запису на вихідний день клініки (операція rescheduleAppointment). */
@ApiIntegrationTest
class RescheduleAppointmentToNonWorkingDayTest {

	@Autowired
	private MockMvc mockMvc;

	private String appointmentId;

	@BeforeEach
	void createFridayAppointment() throws Exception {
		String doctorId = createDoctor(mockMvc, "Вихідні Перенесення Лікар");
		String patientId = createPatientWithOwner(mockMvc, "Вихідні Перенесення Пацієнт");
		appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-10-30", "14:00"));
	}

	private ResultActions reschedule(RequestPostProcessor principal, String id, String startsAt) throws Exception {
		return mockMvc.perform(patch("/api/v1/appointments/" + id)
				.with(principal)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"startsAt\": \"" + startsAt + "\" }"));
	}

	private ResultActions rescheduleAsAdmin(String id, String startsAt) throws Exception {
		return reschedule(user("admin").roles("ADMIN"), id, startsAt);
	}

	@Test
	@DisplayName("AD-25.3: перенесення запису з п'ятниці на суботу (31.10.2026, 14:00) — 422, поле startsAt, причина NON_WORKING_DAY")
	void rescheduleToSaturday_returns422NonWorkingDay() throws Exception {
		// Arrange
		String saturdayStart = slot("2026-10-31", "14:00");

		// Act
		var response = rescheduleAsAdmin(appointmentId, saturdayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.3: перенесення запису з п'ятниці на неділю (01.11.2026, 10:00) — 422, причина NON_WORKING_DAY")
	void rescheduleToSunday_returns422NonWorkingDay() throws Exception {
		// Arrange
		String sundayStart = slot("2026-11-01", "10:00");

		// Act
		var response = rescheduleAsAdmin(appointmentId, sundayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.4: після відхиленого перенесення запис лишається на слоті 14:00 30.10.2026 зі статусом SCHEDULED")
	void rejectedReschedule_keepsAppointmentUnchanged() throws Exception {
		// Arrange
		rescheduleAsAdmin(appointmentId, slot("2026-10-31", "14:00")).andExpect(status().isUnprocessableContent());

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-10-30", "14:00"))))
				.andExpect(jsonPath("$.endsAt", equalTo(slot("2026-10-30", "14:30"))))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-25.1: перенесення з п'ятниці на понеділок (02.11.2026, 09:00) — 200, час новий")
	void rescheduleToMonday_returns200() throws Exception {
		// Arrange
		String mondayStart = slot("2026-11-02", "09:00");

		// Act
		var response = rescheduleAsAdmin(appointmentId, mondayStart);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.startsAt", equalTo(mondayStart)))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-25.3: перенесення на минулу суботу (19.09.2026, 11:00) також відхиляється")
	void rescheduleToPastSaturday_returns422NonWorkingDay() throws Exception {
		// Arrange
		String pastSaturdayStart = slot("2026-09-19", "11:00");

		// Act
		var response = rescheduleAsAdmin(appointmentId, pastSaturdayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-18.4: скасований запис переносять на суботу — 422 NON_WORKING_DAY (422 раніше за 409 APPOINTMENT_NOT_SCHEDULED)")
	void rescheduleCancelledToSaturday_returns422BeforeConflict() throws Exception {
		// Arrange
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = rescheduleAsAdmin(appointmentId, slot("2026-10-31", "14:00"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-18.4: лікар переносить запис на суботу — 403 за роллю")
	void veterinarianRescheduleToSaturday_returns403() throws Exception {
		// Arrange
		var veterinarian = user("vet").roles("VETERINARIAN");

		// Act
		var response = reschedule(veterinarian, appointmentId, slot("2026-10-31", "14:00"));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}
}
