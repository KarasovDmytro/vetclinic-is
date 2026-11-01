package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-015, AD-04.2: архівування пацієнта відхиляється, поки є записи у статусі «Заплановано». */
@ApiIntegrationTest
class PatientArchiveWithAppointmentsTest {

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	private String patientId;

	@BeforeEach
	void createDoctorAndPatient() throws Exception {
		doctorId = createDoctor(mockMvc, "Архів Із Записами Лікар");
		patientId = createPatientWithOwner(mockMvc, "Архів Із Записами Пацієнт");
	}

	private ResultActions archive(String id) throws Exception {
		return mockMvc.perform(post("/api/v1/patients/" + id + "/archive").with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("AD-04.2: у пацієнта є запис SCHEDULED — архівація 409 PATIENT_HAS_SCHEDULED_APPOINTMENTS, картка лишається ACTIVE")
	void patientWithScheduledAppointment_cannotBeArchived() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-17", "10:00"));

		// Act
		var response = archive(patientId);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_HAS_SCHEDULED_APPOINTMENTS")))
				.andExpect(jsonPath("$.message", equalTo("У пацієнта є заплановані записи")))
				.andExpect(jsonPath("$.path", equalTo("/api/v1/patients/" + patientId + "/archive")));
		mockMvc.perform(get("/api/v1/patients/" + patientId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("ACTIVE")));
	}

	@Test
	@DisplayName("AD-04.2: після скасування запису архівація проходить — 200 ARCHIVED")
	void afterCancellingAppointment_patientCanBeArchived() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-17", "10:00"));
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = archive(patientId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("ARCHIVED")));
	}

	@Test
	@DisplayName("AD-04.2: кілька записів — достатньо одного SCHEDULED, щоб заблокувати архівацію")
	void oneRemainingScheduledAppointment_stillBlocksArchive() throws Exception {
		// Arrange
		String first = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-17", "10:00"));
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-17", "11:00"));
		cancelAppointment(mockMvc, first);

		// Act
		var response = archive(patientId);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_HAS_SCHEDULED_APPOINTMENTS")));
	}

	@Test
	@DisplayName("AD-04.2: перенесений запис лишається SCHEDULED — архівація й далі заблокована")
	void rescheduledAppointment_stillBlocksArchive() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-17", "10:00"));
		mockMvc.perform(patch("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"startsAt\": \"" + slot("2026-12-17", "12:00") + "\" }"))
				.andExpect(status().isOk());

		// Act
		var response = archive(patientId);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_HAS_SCHEDULED_APPOINTMENTS")));
	}

	@Test
	@DisplayName("AD-04.2: записи іншого пацієнта не блокують архівацію")
	void appointmentsOfOtherPatient_doNotBlockArchive() throws Exception {
		// Arrange
		String otherPatient = createPatientWithOwner(mockMvc, "Архів Із Записами Інший Пацієнт");
		createAppointment(mockMvc, doctorId, otherPatient, slot("2026-12-17", "10:00"));

		// Act
		var response = archive(patientId);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-04.2: пацієнт без записів архівується — 200")
	void patientWithoutAppointments_canBeArchived() throws Exception {
		// Arrange
		String id = patientId;

		// Act
		var response = archive(id);

		// Assert
		response.andExpect(status().isOk());
	}
}
