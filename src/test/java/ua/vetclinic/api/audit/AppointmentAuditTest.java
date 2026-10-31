package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-NF-006, AD-22, AD-23, AD-24.2: записи на прийом фіксуються в журналі аудиту лише ідентифікаторами. */
@ApiIntegrationTest
class AppointmentAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	private String adminId;

	private String adminToken;

	private String doctorId;

	private String patientId;

	@BeforeEach
	void startCaptureAndLogInAdmin() throws Exception {
		String adminEmail = "audit.appointments." + System.nanoTime() + "@clinic.example";
		adminId = createUser(mockMvc, adminEmail, "Адмін Аудиту Записів", "ADMIN", "Admin-Passw0rd-2026");
		adminToken = loginAndGetToken(mockMvc, adminEmail, "Admin-Passw0rd-2026");
		doctorId = createDoctor(mockMvc, "Аудит Записів Лікар");
		patientId = createPatientWithOwner(mockMvc, "Секретна Кличка Запису");
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	private org.springframework.test.web.servlet.ResultActions create(String startsAt, String patient)
			throws Exception {
		return mockMvc.perform(post("/api/v1/appointments")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s" }
						""".formatted(doctorId, patient, startsAt)));
	}

	@Test
	@DisplayName("REQ-NF-006: створення запису — подія APPOINTMENT_CREATED з виконавцем і сутністю, без імені пацієнта")
	void createAppointment_isAudited() throws Exception {
		// Arrange
		String startsAt = slot("2026-12-21", "10:00");

		// Act
		var result = create(startsAt, patientId).andExpect(status().isCreated()).andReturn();
		String appointmentId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CREATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=APPOINTMENT"),
				containsString("entityId=" + appointmentId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
		assertThat(audit.lines(), everyItem(not(containsString("Секретна Кличка"))));
	}

	@Test
	@DisplayName("AD-22.4: зайнятий слот (409) — APPOINTMENT_CREATED з outcome=FAILURE і errorCode SLOT_TAKEN")
	void slotTaken_isAuditedAsFailure() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "11:00"));

		// Act
		create(slot("2026-12-21", "11:00"), patientId).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CREATED"),
				containsString("actorId=" + adminId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=SLOT_TAKEN"))));
	}

	@Test
	@DisplayName("AD-22.4: архівний пацієнт (409) — APPOINTMENT_CREATED з outcome=FAILURE і errorCode PATIENT_ARCHIVED")
	void archivedPatient_isAuditedAsFailure() throws Exception {
		// Arrange
		String archivedPatient = createPatientWithOwner(mockMvc, "Аудит Архівний Пацієнт");
		mockMvc.perform(post("/api/v1/patients/" + archivedPatient + "/archive")
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

		// Act
		create(slot("2026-12-21", "12:00"), archivedPatient).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CREATED"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=PATIENT_ARCHIVED"))));
	}

	@Test
	@DisplayName("AD-22.4: незбіг слота (422) — APPOINTMENT_CREATED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void misalignedSlot_isAuditedAsFailure() throws Exception {
		// Arrange
		String startsAt = slot("2026-12-21", "12:10");

		// Act
		create(startsAt, patientId).andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CREATED"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("REQ-NF-006: перенесення запису — подія APPOINTMENT_RESCHEDULED з виконавцем і сутністю")
	void rescheduleAppointment_isAudited() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "13:00"));

		// Act
		var result = mockMvc.perform(patch("/api/v1/appointments/" + appointmentId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"startsAt\": \"" + slot("2026-12-21", "14:00") + "\" }"))
				.andExpect(status().isOk()).andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_RESCHEDULED"),
				containsString("actorId=" + adminId),
				containsString("entityType=APPOINTMENT"),
				containsString("entityId=" + appointmentId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-22.4: перенесення на зайнятий слот (409) — APPOINTMENT_RESCHEDULED з outcome=FAILURE і errorCode SLOT_TAKEN")
	void rescheduleToTakenSlot_isAuditedAsFailure() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "15:00"));
		String otherPatient = createPatientWithOwner(mockMvc, "Аудит Перенесення Інший Пацієнт");
		createAppointment(mockMvc, doctorId, otherPatient, slot("2026-12-21", "16:00"));

		// Act
		mockMvc.perform(patch("/api/v1/appointments/" + appointmentId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"startsAt\": \"" + slot("2026-12-21", "16:00") + "\" }"))
				.andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_RESCHEDULED"),
				containsString("entityId=" + appointmentId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=SLOT_TAKEN"))));
	}

	@Test
	@DisplayName("REQ-NF-006: скасування запису — подія APPOINTMENT_CANCELLED з виконавцем і сутністю")
	void cancelAppointment_isAudited() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "09:00"));

		// Act
		var result = mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/cancel")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk()).andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CANCELLED"),
				containsString("actorId=" + adminId),
				containsString("entityType=APPOINTMENT"),
				containsString("entityId=" + appointmentId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-22.4: повторне скасування (409) — APPOINTMENT_CANCELLED з outcome=FAILURE і errorCode APPOINTMENT_NOT_SCHEDULED")
	void cancelTwice_isAuditedAsFailure() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "09:30"));
		cancelAppointment(mockMvc, appointmentId);

		// Act
		mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/cancel")
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=APPOINTMENT_CANCELLED"),
				containsString("entityId=" + appointmentId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=APPOINTMENT_NOT_SCHEDULED"))));
	}

	@Test
	@DisplayName("AD-22.4: архівація пацієнта із запланованим записом (409) — PATIENT_ARCHIVED з outcome=FAILURE")
	void archiveWithScheduledAppointment_isAuditedAsFailure() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "10:30"));

		// Act
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive")
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_ARCHIVED"),
				containsString("entityId=" + patientId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=PATIENT_HAS_SCHEDULED_APPOINTMENTS"))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.1: читання розкладу, списку й запису не фіксується в журналі аудиту")
	void readingSchedule_isNotAudited() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "17:00"));
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule").param("from", "2026-12-21")
				.param("to", "2026-12-21").header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/appointments").param("doctorId", doctorId)
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId)
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/doctors").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}
}
