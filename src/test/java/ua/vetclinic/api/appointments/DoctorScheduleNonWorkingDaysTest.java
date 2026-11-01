package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.slot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-020, REQ-F-049, AD-25.6: сітка розкладу не містить слотів у вихідні дні (операція getDoctorSchedule). */
@ApiIntegrationTest
class DoctorScheduleNonWorkingDaysTest {

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	@BeforeEach
	void createDoctorWithoutAppointments() throws Exception {
		doctorId = createDoctor(mockMvc, "Вихідні Розклад Лікар");
	}

	private ResultActions schedule(String from, String to) throws Exception {
		return mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", from)
				.param("to", to)
				.with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("AD-25.6: діапазон п'ятниця–понеділок (30.10–02.11.2026) — 36 слотів: 18 п'ятниці й 18 понеділка")
	void fridayToMonday_hasOnlyWorkingDaySlots() throws Exception {
		// Arrange
		String from = "2026-10-30";
		String to = "2026-11-02";

		// Act
		var response = schedule(from, to);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.from", equalTo(from)))
				.andExpect(jsonPath("$.to", equalTo(to)))
				.andExpect(jsonPath("$.slots", hasSize(36)))
				.andExpect(jsonPath("$.slots[0].startsAt", equalTo(slot("2026-10-30", "09:00"))))
				.andExpect(jsonPath("$.slots[17].startsAt", equalTo(slot("2026-10-30", "17:30"))))
				.andExpect(jsonPath("$.slots[18].startsAt", equalTo(slot("2026-11-02", "09:00"))))
				.andExpect(jsonPath("$.slots[35].startsAt", equalTo(slot("2026-11-02", "17:30"))));
	}

	@Test
	@DisplayName("AD-25.6: діапазон лише з вихідних (31.10–01.11.2026) — 200 і порожній список слотів")
	void weekendOnlyRange_returnsEmptySlots() throws Exception {
		// Arrange
		String from = "2026-10-31";
		String to = "2026-11-01";

		// Act
		var response = schedule(from, to);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.from", equalTo(from)))
				.andExpect(jsonPath("$.to", equalTo(to)))
				.andExpect(jsonPath("$.slotMinutes", equalTo(30)))
				.andExpect(jsonPath("$.slots", empty()));
	}

	@Test
	@DisplayName("AD-25.6: один день — субота (31.10.2026) — 200 і порожній список слотів")
	void singleSaturday_returnsEmptySlots() throws Exception {
		// Arrange
		String saturday = "2026-10-31";

		// Act
		var response = schedule(saturday, saturday);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", empty()));
	}

	@Test
	@DisplayName("AD-25.6: тиждень понеділок–неділя (02.11–08.11.2026) — 90 слотів (5 робочих днів по 18)")
	void fullWeek_hasNinetySlots() throws Exception {
		// Arrange
		String monday = "2026-11-02";
		String sunday = "2026-11-08";

		// Act
		var response = schedule(monday, sunday);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(90)))
				.andExpect(jsonPath("$.slots[0].startsAt", equalTo(slot("2026-11-02", "09:00"))))
				.andExpect(jsonPath("$.slots[89].startsAt", equalTo(slot("2026-11-06", "17:30"))));
	}

	@Test
	@DisplayName("AD-25.6: лікар бачить ту саму сітку — діапазон п'ятниця–понеділок дає 36 слотів")
	void veterinarianSeesSameGrid() throws Exception {
		// Arrange
		var veterinarian = user("vet").roles("VETERINARIAN");

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", "2026-10-30")
				.param("to", "2026-11-02")
				.with(veterinarian));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(36)));
	}
}
