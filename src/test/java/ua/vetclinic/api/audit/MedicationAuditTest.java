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
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-NF-006, AD-22, AD-24.2: зміни каталогу медикаментів фіксуються в журналі аудиту без зайвих даних. */
@ApiIntegrationTest
class MedicationAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	private String adminId;

	private String adminToken;

	@BeforeEach
	void startCaptureAndLogInAdmin() throws Exception {
		String adminEmail = "audit.medications." + System.nanoTime() + "@clinic.example";
		adminId = createUser(mockMvc, adminEmail, "Адмін Аудиту Медикаментів", "ADMIN", "Admin-Passw0rd-2026");
		adminToken = loginAndGetToken(mockMvc, adminEmail, "Admin-Passw0rd-2026");
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	private ResultActions asAdmin(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
			String body) throws Exception {
		return mockMvc.perform(request.header("Authorization", "Bearer " + adminToken)
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	@DisplayName("REQ-NF-006: додавання медикамента — подія MEDICATION_CREATED з виконавцем і сутністю")
	void createMedication_isAudited() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Аудит") + "\", \"quantity\": 4 }";

		// Act
		var result = asAdmin(post("/api/v1/medications"), requestBody).andExpect(status().isCreated()).andReturn();
		String medicationId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_CREATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=MEDICATION"),
				containsString("entityId=" + medicationId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-22.4: дубль назви (409) — MEDICATION_CREATED з outcome=FAILURE і errorCode MEDICATION_NAME_TAKEN")
	void duplicateName_isAuditedAsFailure() throws Exception {
		// Arrange
		String name = uniqueName("Аудит дубль");
		createMedication(mockMvc, name, 1, null);

		// Act
		asAdmin(post("/api/v1/medications"), "{ \"name\": \"" + name + "\", \"quantity\": 1 }")
				.andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_CREATED"),
				containsString("actorId=" + adminId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=MEDICATION_NAME_TAKEN"))));
	}

	@Test
	@DisplayName("REQ-NF-006: редагування медикамента — подія MEDICATION_UPDATED з виконавцем і сутністю")
	void updateMedication_isAudited() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит редагування"), 1, null);

		// Act
		var result = asAdmin(patch("/api/v1/medications/" + medicationId), "{ \"minThreshold\": 2 }")
				.andExpect(status().isOk()).andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_UPDATED"),
				containsString("actorId=" + adminId),
				containsString("entityType=MEDICATION"),
				containsString("entityId=" + medicationId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-22.4: редагування на зайняту назву (409) — MEDICATION_UPDATED з outcome=FAILURE і errorCode MEDICATION_NAME_TAKEN")
	void renameToTakenName_isAuditedAsFailure() throws Exception {
		// Arrange
		String takenName = uniqueName("Аудит зайнята");
		createMedication(mockMvc, takenName, 1, null);
		String medicationId = createMedication(mockMvc, uniqueName("Аудит перейменування"), 1, null);

		// Act
		asAdmin(patch("/api/v1/medications/" + medicationId), "{ \"name\": \"" + takenName + "\" }")
				.andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_UPDATED"),
				containsString("entityId=" + medicationId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=MEDICATION_NAME_TAKEN"))));
	}

	@Test
	@DisplayName("AD-22.4: спроба змінити залишок через PATCH (422) — MEDICATION_UPDATED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void quantityInPatch_isAuditedAsFailure() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит залишок"), 1, null);

		// Act
		asAdmin(patch("/api/v1/medications/" + medicationId), "{ \"quantity\": 50 }")
				.andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_UPDATED"),
				containsString("entityId=" + medicationId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("REQ-NF-006: коригування залишку — подія MEDICATION_STOCK_ADJUSTED з виконавцем і сутністю")
	void adjustStock_isAudited() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит коригування"), 5, null);

		// Act
		var result = asAdmin(post("/api/v1/medications/" + medicationId + "/adjust-stock"), "{ \"delta\": 3 }")
				.andExpect(status().isOk()).andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_STOCK_ADJUSTED"),
				containsString("actorId=" + adminId),
				containsString("entityType=MEDICATION"),
				containsString("entityId=" + medicationId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("AD-22.4: від'ємний залишок (409) — MEDICATION_STOCK_ADJUSTED з outcome=FAILURE і errorCode NEGATIVE_STOCK")
	void negativeStock_isAuditedAsFailure() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит мінус"), 1, null);

		// Act
		asAdmin(post("/api/v1/medications/" + medicationId + "/adjust-stock"), "{ \"delta\": -5 }")
				.andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_STOCK_ADJUSTED"),
				containsString("entityId=" + medicationId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=NEGATIVE_STOCK"))));
	}

	@Test
	@DisplayName("AD-22.4: нульова зміна (422) — MEDICATION_STOCK_ADJUSTED з outcome=FAILURE і errorCode VALIDATION_ERROR")
	void zeroDelta_isAuditedAsFailure() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит нуль"), 1, null);

		// Act
		asAdmin(post("/api/v1/medications/" + medicationId + "/adjust-stock"), "{ \"delta\": 0 }")
				.andExpect(status().isUnprocessableContent());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=MEDICATION_STOCK_ADJUSTED"),
				containsString("entityId=" + medicationId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=VALIDATION_ERROR"))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.1: читання каталогу не фіксується в журналі аудиту")
	void readingCatalog_isNotAudited() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит читання"), 1, null);
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(get("/api/v1/medications").header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/medications/" + medicationId).header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}

	@Test
	@DisplayName("AD-23: назва медикамента не потрапляє в журнал аудиту")
	void medicationName_isNotLogged() throws Exception {
		// Arrange
		String secretName = "СекретнийПрепарат" + System.nanoTime();

		// Act
		asAdmin(post("/api/v1/medications"), "{ \"name\": \"" + secretName + "\", \"quantity\": 1 }")
				.andExpect(status().isCreated());

		// Assert
		assertThat(audit.lines(), everyItem(not(containsString(secretName))));
	}
}
