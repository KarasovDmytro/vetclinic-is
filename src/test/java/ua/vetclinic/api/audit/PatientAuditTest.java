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
import static ua.vetclinic.api.TestOwners.createOwner;
import static ua.vetclinic.api.TestPatients.createPatient;

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

/** REQ-NF-006, AD-22, AD-23, AD-24.2: зміни карток пацієнтів фіксуються в журналі аудиту лише ідентифікаторами. */
@ApiIntegrationTest
class PatientAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	private String adminId;

	private String adminToken;

	private String ownerId;

	@BeforeEach
	void startCaptureAndLogInAdmin() throws Exception {
		String adminEmail = "audit.patients." + System.nanoTime() + "@clinic.example";
		adminId = createUser(mockMvc, adminEmail, "Адмін Аудиту Пацієнтів", "ADMIN", "Admin-Passw0rd-2026");
		adminToken = loginAndGetToken(mockMvc, adminEmail, "Admin-Passw0rd-2026");
		ownerId = createOwner(mockMvc, "Власник Аудиту Пацієнтів", "+380501234660");
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	@Test
	@DisplayName("REQ-NF-006: реєстрація пацієнта — подія PATIENT_CREATED з виконавцем і сутністю, без імені тварини")
	void createPatient_isAuditedWithoutPersonalData() throws Exception {
		// Arrange
		String requestBody = "{ \"ownerId\": \"" + ownerId + "\", \"name\": \"Секретна Кличка\", \"species\": \"Кіт\" }";

		// Act
		var result = mockMvc.perform(post("/api/v1/patients")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		String patientId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_CREATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=PATIENT"),
				containsString("entityId=" + patientId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
		assertThat(audit.lines(), everyItem(not(containsString("Секретна Кличка"))));
	}

	@Test
	@DisplayName("REQ-NF-006: редагування пацієнта — подія PATIENT_UPDATED з виконавцем і сутністю, без нового імені")
	void updatePatient_isAuditedWithoutPersonalData() throws Exception {
		// Arrange
		String patientId = createPatient(mockMvc, ownerId, "Стара Кличка", "Кіт");

		// Act
		var result = mockMvc.perform(patch("/api/v1/patients/" + patientId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Нова Секретна Кличка\" }"))
				.andExpect(status().isOk())
				.andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_UPDATED"),
				containsString("actorId=" + adminId),
				containsString("entityType=PATIENT"),
				containsString("entityId=" + patientId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
		assertThat(audit.lines(), everyItem(not(containsString("Секретна"))));
	}

	@Test
	@DisplayName("REQ-NF-006: архівація картки — подія PATIENT_ARCHIVED з виконавцем і сутністю")
	void archivePatient_isAudited() throws Exception {
		// Arrange
		String patientId = createPatient(mockMvc, ownerId, "Архівація Аудит", "Кіт");

		// Act
		var result = mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk())
				.andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_ARCHIVED"),
				containsString("actorId=" + adminId),
				containsString("entityType=PATIENT"),
				containsString("entityId=" + patientId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-22.4: повторна архівація (409) — PATIENT_ARCHIVED з outcome=FAILURE і errorCode PATIENT_ARCHIVED")
	void archiveTwice_isAuditedAsFailure() throws Exception {
		// Arrange
		String patientId = createPatient(mockMvc, ownerId, "Подвійна Архівація", "Кіт");
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive")
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

		// Act
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive")
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_ARCHIVED"),
				containsString("actorId=" + adminId),
				containsString("entityId=" + patientId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=PATIENT_ARCHIVED"))));
	}

	@Test
	@DisplayName("AD-22.4: реєстрація з неіснуючим власником (422) — PATIENT_CREATED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void createWithUnknownOwner_isAuditedAsFailure() throws Exception {
		// Arrange
		String requestBody = "{ \"ownerId\": \"00000000-0000-4000-8000-000000000551\", \"name\": \"Сирота\", "
				+ "\"species\": \"Кіт\" }";

		// Act
		mockMvc.perform(post("/api/v1/patients")
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody)).andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_CREATED"),
				containsString("actorId=" + adminId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("AD-22.4: спроба змінити власника (422 IMMUTABLE_FIELD) — PATIENT_UPDATED з outcome=FAILURE")
	void changingOwner_isAuditedAsFailure() throws Exception {
		// Arrange
		String patientId = createPatient(mockMvc, ownerId, "Спроба Змінити", "Кіт");

		// Act
		mockMvc.perform(patch("/api/v1/patients/" + patientId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"ownerId\": \"" + ownerId + "\" }")).andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_UPDATED"),
				containsString("entityId=" + patientId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("AD-22.4: порожнє тіло PATCH (422 EMPTY_BODY) — PATIENT_UPDATED з outcome=FAILURE")
	void emptyUpdateBody_isAuditedAsFailure() throws Exception {
		// Arrange
		String patientId = createPatient(mockMvc, ownerId, "Порожня Правка", "Кіт");

		// Act
		mockMvc.perform(patch("/api/v1/patients/" + patientId)
				.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}")).andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=PATIENT_UPDATED"),
				containsString("entityId=" + patientId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.1: читання пацієнтів не фіксується в журналі аудиту")
	void readingPatients_isNotAudited() throws Exception {
		// Arrange
		String patientId = createPatient(mockMvc, ownerId, "Лише Читання", "Кіт");
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(get("/api/v1/patients/" + patientId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/patients").param("q", "Лише Читання")
				.header("Authorization", "Bearer " + adminToken)).andExpect(status().isOk());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}
}
