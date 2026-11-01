package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-F-049, AD-22.4, AD-25: відхилення запису чи перенесення на вихідний день фіксується в журналі аудиту. */
@ApiIntegrationTest
class NonWorkingDayAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	private String adminToken;

	private String doctorId;

	private String patientId;

	@BeforeEach
	void startCaptureAndLogInAdmin() throws Exception {
		String adminEmail = "audit.weekend." + System.nanoTime() + "@clinic.example";
		createUser(mockMvc, adminEmail, "Адмін Аудиту Вихідних", "ADMIN", "Admin-Passw0rd-2026");
		adminToken = loginAndGetToken(mockMvc, adminEmail, "Admin-Passw0rd-2026");
		doctorId = createDoctor(mockMvc, "Аудит Вихідних Лікар");
		patientId = createPatientWithOwner(mockMvc, "Аудит Вихідних Пацієнт");
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	@Test
	@DisplayName("AD-22.4: запис на суботу (422) — APPOINTMENT_CREATED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void createOnSaturday_isAuditedAsFailure() throws Exception {
		// Arrange
		String saturdayStart = slot("2026-10-31", "14:00");

		// Act
		mockMvc.perform(post("/api/v1/appointments")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"doctorId\": \"" + doctorId + "\", \"patientId\": \"" + patientId
						+ "\", \"startsAt\": \"" + saturdayStart + "\" }"))
				.andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CREATED"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("AD-22.4: перенесення на суботу (422) — APPOINTMENT_RESCHEDULED з outcome=FAILURE, errorCode VALIDATION_ERROR і id запису")
	void rescheduleToSaturday_isAuditedAsFailure() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-10-30", "14:00"));

		// Act
		mockMvc.perform(patch("/api/v1/appointments/" + appointmentId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"startsAt\": \"" + slot("2026-10-31", "14:00") + "\" }"))
				.andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_RESCHEDULED"),
				containsString("entityId=" + appointmentId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}
}
