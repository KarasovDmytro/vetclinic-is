package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
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

/** REQ-F-049, AD-25: створення запису на вихідний день клініки (операція createAppointment). */
@ApiIntegrationTest
class CreateAppointmentOnNonWorkingDayTest {

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	private String patientId;

	@BeforeEach
	void createDoctorAndPatient() throws Exception {
		doctorId = createDoctor(mockMvc, "Вихідні Створення Лікар");
		patientId = createPatientWithOwner(mockMvc, "Вихідні Створення Пацієнт");
	}

	private ResultActions postAppointment(String startsAt) throws Exception {
		return postAppointmentAs(user("admin").roles("ADMIN"), startsAt);
	}

	private ResultActions postAppointmentAs(RequestPostProcessor principal, String startsAt) throws Exception {
		return mockMvc.perform(post("/api/v1/appointments")
				.with(principal)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"doctorId\": \"" + doctorId + "\", \"patientId\": \"" + patientId
						+ "\", \"startsAt\": \"" + startsAt + "\" }"));
	}

	@Test
	@DisplayName("AD-25.4: запис на суботу (31.10.2026, 14:00) — 422, поле startsAt, причина NON_WORKING_DAY")
	void saturday_returns422NonWorkingDay() throws Exception {
		// Arrange
		String saturdayStart = slot("2026-10-31", "14:00");

		// Act
		var response = postAppointment(saturdayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.4: запис на неділю (01.11.2026, 10:00) — 422, поле startsAt, причина NON_WORKING_DAY")
	void sunday_returns422NonWorkingDay() throws Exception {
		// Arrange
		String sundayStart = slot("2026-11-01", "10:00");

		// Act
		var response = postAppointment(sundayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.4: після відхилення запису на вихідний день запис не з'являється в базі")
	void rejectedWeekendAppointment_isNotStored() throws Exception {
		// Arrange
		postAppointment(slot("2026-10-31", "14:00")).andExpect(status().isUnprocessableContent());

		// Act
		var response = mockMvc.perform(get("/api/v1/appointments")
				.param("doctorId", doctorId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)))
				.andExpect(jsonPath("$.items", empty()));
	}

	@Test
	@DisplayName("AD-25.1: п'ятниця (30.10.2026, 14:00) — останній робочий день перед вихідними, запис створено")
	void friday_isCreated() throws Exception {
		// Arrange
		String fridayStart = slot("2026-10-30", "14:00");

		// Act
		var response = postAppointment(fridayStart);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.startsAt", equalTo(fridayStart)));
	}

	@Test
	@DisplayName("AD-25.1: понеділок (02.11.2026, 09:00) — перший робочий день після вихідних, запис створено")
	void monday_isCreated() throws Exception {
		// Arrange
		String mondayStart = slot("2026-11-02", "09:00");

		// Act
		var response = postAppointment(mondayStart);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.startsAt", equalTo(mondayStart)));
	}

	@Test
	@DisplayName("AD-25.2: день рахується за часом клініки — 2026-10-31T07:00:00Z це субота 09:00 за Києвом, 422")
	void utcInstantThatIsSaturdayInClinicZone_returns422NonWorkingDay() throws Exception {
		// Arrange
		String saturdayMorningUtc = "2026-10-31T07:00:00Z";

		// Act
		var response = postAppointment(saturdayMorningUtc);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.2: п'ятниця за UTC, але субота 00:30 за Києвом (2026-10-30T22:30:00Z) — NON_WORKING_DAY")
	void fridayInUtcButSaturdayInClinicZone_returns422NonWorkingDay() throws Exception {
		// Arrange
		String fridayNightUtc = "2026-10-30T22:30:00Z";

		// Act
		var response = postAppointment(fridayNightUtc);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.2: неділя за UTC, але понеділок 00:30 за Києвом (2026-11-01T22:30:00Z) — робочий день, поза годинами")
	void sundayInUtcButMondayInClinicZone_returns422OutsideWorkingHours() throws Exception {
		// Arrange
		String sundayNightUtc = "2026-11-01T22:30:00Z";

		// Act
		var response = postAppointment(sundayNightUtc);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUTSIDE_WORKING_HOURS")));
	}

	@Test
	@DisplayName("AD-25.3: правило діє і для минулих дат — запис на неділю 20.09.2026 відхиляється")
	void pastSunday_returns422NonWorkingDay() throws Exception {
		// Arrange
		String pastSundayStart = slot("2026-09-20", "11:00");

		// Act
		var response = postAppointment(pastSundayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.5: субота 14:10 — лише SLOT_NOT_ALIGNED (вирівнювання перевіряється раніше за день)")
	void saturdayMisaligned_returnsOnlySlotNotAligned() throws Exception {
		// Arrange
		String misalignedSaturday = slot("2026-10-31", "14:10");

		// Act
		var response = postAppointment(misalignedSaturday);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("SLOT_NOT_ALIGNED")));
	}

	@Test
	@DisplayName("AD-25.5: субота 20:00 — лише NON_WORKING_DAY, а не OUTSIDE_WORKING_HOURS (день перевіряється раніше за години)")
	void saturdayAfterHours_returnsOnlyNonWorkingDay() throws Exception {
		// Arrange
		String lateSaturday = slot("2026-10-31", "20:00");

		// Act
		var response = postAppointment(lateSaturday);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-18.4: лікар створює запис на суботу — 403 за роллю, правило вихідних не розкриває себе")
	void veterinarianOnSaturday_returns403() throws Exception {
		// Arrange
		var veterinarian = user("vet").roles("VETERINARIAN");

		// Act
		var response = postAppointmentAs(veterinarian, slot("2026-10-31", "14:00"));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}
}
