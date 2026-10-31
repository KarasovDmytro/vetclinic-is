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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.createVisit;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.postVisit;
import static ua.vetclinic.api.TestVisits.visitBodyWith;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-NF-006, AD-22, AD-23: прийоми фіксуються в журналі аудиту лише ідентифікаторами, без медичних даних. */
@ApiIntegrationTest
class VisitAuditTest {

	private static final String SECRET_DIAGNOSIS = "СекретнийДіагноз";

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Doctor doctor;

	private String patientId;

	private String appointmentId;

	@BeforeEach
	void scheduleAppointmentAndStartCapture() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Аудиту Прийомів");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Аудиту Прийомів");
		appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-11-30", "10:00"));
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	@Test
	@DisplayName("REQ-NF-006: збереження прийому — подія VISIT_CREATED з лікарем, сутністю VISIT і requestId")
	void createVisit_isAudited() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"" + SECRET_DIAGNOSIS + "\" }";

		// Act
		var result = postVisit(mockMvc, doctor, appointmentId, requestBody).andExpect(status().isCreated())
				.andReturn();
		String visitId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_CREATED"),
				containsString("actorId=" + doctor.id()),
				containsString("actorRole=VETERINARIAN"),
				containsString("entityType=VISIT"),
				containsString("entityId=" + visitId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-23: діагноз, призначення, нотатки й назви препаратів не потрапляють у журнал")
	void medicalData_isNotLogged() throws Exception {
		// Arrange
		String medicationName = uniqueName("СекретнийПрепарат");
		String medicationId = createMedication(mockMvc, medicationName, 5, null);
		String requestBody = "{ \"diagnosis\": \"" + SECRET_DIAGNOSIS + "\", \"prescriptions\": \"СекретніПризначення\", "
				+ "\"notes\": \"СекретніНотатки\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\", \"quantity\": 1 } ] }";

		// Act
		postVisit(mockMvc, doctor, appointmentId, requestBody).andExpect(status().isCreated());

		// Assert
		assertThat(audit.lines(), everyItem(not(containsString(SECRET_DIAGNOSIS))));
		assertThat(audit.lines(), everyItem(not(containsString("СекретніПризначення"))));
		assertThat(audit.lines(), everyItem(not(containsString("СекретніНотатки"))));
		assertThat(audit.lines(), everyItem(not(containsString(medicationName))));
	}

	@Test
	@DisplayName("AD-22.4: нестача препарату (409) — VISIT_CREATED з outcome=FAILURE і errorCode INSUFFICIENT_STOCK")
	void insufficientStock_isAuditedAsFailure() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит нестача"), 1, null);

		// Act
		postVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, 5)).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_CREATED"),
				containsString("actorId=" + doctor.id()),
				containsString("outcome=FAILURE"),
				containsString("errorCode=INSUFFICIENT_STOCK"))));
	}

	@Test
	@DisplayName("AD-22.4: чужий лікар (403) — VISIT_CREATED з outcome=FAILURE і errorCode NOT_ASSIGNED_DOCTOR")
	void otherDoctor_isAuditedAsFailure() throws Exception {
		// Arrange
		Doctor other = newDoctor(mockMvc, "Лікар Аудиту Чужий");

		// Act
		postVisit(mockMvc, other, appointmentId, "{ \"diagnosis\": \"Діагноз\" }").andExpect(status().isForbidden());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_CREATED"),
				containsString("actorId=" + other.id()),
				containsString("outcome=FAILURE"),
				containsString("errorCode=NOT_ASSIGNED_DOCTOR"))));
	}

	@Test
	@DisplayName("AD-22.4: скасований запис (409) — VISIT_CREATED з outcome=FAILURE і errorCode APPOINTMENT_NOT_SCHEDULED")
	void cancelledAppointment_isAuditedAsFailure() throws Exception {
		// Arrange
		cancelAppointment(mockMvc, appointmentId);

		// Act
		postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Діагноз\" }").andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_CREATED"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=APPOINTMENT_NOT_SCHEDULED"))));
	}

	@Test
	@DisplayName("AD-22.4: архівна картка (409) — VISIT_CREATED з outcome=FAILURE і errorCode PATIENT_ARCHIVED")
	void archivedPatient_isAuditedAsFailure() throws Exception {
		// Arrange
		jdbcTemplate.update("update patients set status = 'ARCHIVED' where id = ?::uuid", patientId);

		// Act
		postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Діагноз\" }").andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_CREATED"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=PATIENT_ARCHIVED"))));
	}

	@Test
	@DisplayName("AD-22.4: неіснуючий препарат у переліку (422) — VISIT_CREATED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void unknownMedication_isAuditedAsFailure() throws Exception {
		// Arrange
		String requestBody = visitBodyWith("00000000-0000-4000-8000-000000000b0b", 1);

		// Act
		postVisit(mockMvc, doctor, appointmentId, requestBody).andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_CREATED"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("REQ-NF-006: редагування прийому — подія VISIT_UPDATED з автором і сутністю VISIT")
	void updateVisit_isAudited() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);

		// Act
		var result = mockMvc.perform(patch("/api/v1/visits/" + visitId)
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"" + SECRET_DIAGNOSIS + "\" }"))
				.andExpect(status().isOk()).andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_UPDATED"),
				containsString("actorId=" + doctor.id()),
				containsString("entityType=VISIT"),
				containsString("entityId=" + visitId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
		assertThat(audit.lines(), everyItem(not(containsString(SECRET_DIAGNOSIS))));
	}

	@Test
	@DisplayName("AD-22.4: редагування чужого прийому (403) — VISIT_UPDATED з outcome=FAILURE і errorCode NOT_VISIT_AUTHOR")
	void updateByOtherDoctor_isAuditedAsFailure() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);
		Doctor other = newDoctor(mockMvc, "Лікар Аудиту Редагування Чужий");

		// Act
		mockMvc.perform(patch("/api/v1/visits/" + visitId)
				.header("Authorization", other.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Чужа правка\" }"))
				.andExpect(status().isForbidden());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_UPDATED"),
				containsString("actorId=" + other.id()),
				containsString("entityId=" + visitId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=NOT_VISIT_AUTHOR"))));
	}

	@Test
	@DisplayName("AD-22.4: редагування на архівній картці (409) — VISIT_UPDATED з outcome=FAILURE і errorCode PATIENT_ARCHIVED")
	void updateOnArchivedCard_isAuditedAsFailure() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);
		jdbcTemplate.update("update patients set status = 'ARCHIVED' where id = ?::uuid", patientId);

		// Act
		mockMvc.perform(patch("/api/v1/visits/" + visitId)
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Правка\" }"))
				.andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_UPDATED"),
				containsString("entityId=" + visitId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=PATIENT_ARCHIVED"))));
	}

	@Test
	@DisplayName("AD-22.4: спроба змінити перелік препаратів (422) — VISIT_UPDATED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void updateMedications_isAuditedAsFailure() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);

		// Act
		mockMvc.perform(patch("/api/v1/visits/" + visitId)
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"medications\": [] }"))
				.andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=VISIT_UPDATED"),
				containsString("entityId=" + visitId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.1: читання прийому й хронології не фіксується в журналі аудиту")
	void readingVisits_isNotAudited() throws Exception {
		// Arrange
		String visitId = createVisit(mockMvc, doctor, appointmentId);
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(get("/api/v1/visits/" + visitId).header("Authorization", doctor.bearer()))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/patients/" + patientId + "/visits").header("Authorization", doctor.bearer()))
				.andExpect(status().isOk());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}
}
