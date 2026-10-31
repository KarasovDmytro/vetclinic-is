package ua.vetclinic.api.patients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;
import static ua.vetclinic.api.TestOwners.createOwner;
import static ua.vetclinic.api.TestPatients.createPatient;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-004, REQ-NF-002, AD-18, AD-19: доступ за ролями і конвенції API для пацієнтів. */
@ApiIntegrationTest
class PatientAccessAndContractTest {

	private static final String CLINIC_OFFSET_TIMESTAMP =
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+0[23]:00";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-19.1: createdAt і updatedAt пацієнта — ISO 8601 із зсувом часового поясу клініки, при створенні рівні")
	void timestamps_areIsoWithClinicOffsetAndEqualOnCreation() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Часу", "+380501234650");
		String requestBody = "{ \"ownerId\": \"" + ownerId + "\", \"name\": \"Час\", \"species\": \"Кіт\" }";

		// Act
		var response = mockMvc.perform(post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		var result = response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andReturn();
		String body = result.getResponse().getContentAsString();
		assertThat((String) JsonPath.read(body, "$.updatedAt"), equalTo(JsonPath.read(body, "$.createdAt")));
	}

	@Test
	@DisplayName("REQ-NF-002: POST /patients без токена — 401 UNAUTHORIZED із WWW-Authenticate")
	void createWithoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/patients")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"ownerId\": \"00000000-0000-4000-8000-000000000541\", \"name\": \"Анонім\", "
						+ "\"species\": \"Кіт\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", equalTo("Bearer")))
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-F-004: справжній токен лікаря дозволяє читати пацієнтів, але не створювати, редагувати й архівувати")
	void veterinarianToken_readsButCannotChangePatients() throws Exception {
		// Arrange
		createUser(mockMvc, "patients.vet@clinic.example", "Лікар Пацієнтів Петрович", "VETERINARIAN",
				"Vet-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "patients.vet@clinic.example", "Vet-Passw0rd-2026");
		String ownerId = createOwner(mockMvc, "Власник Токена", "+380501234651");
		String patientId = createPatient(mockMvc, ownerId, "Пацієнт Токена", "Кіт");

		// Act
		var read = mockMvc.perform(get("/api/v1/patients/" + patientId).header("Authorization", "Bearer " + token));
		var list = mockMvc.perform(get("/api/v1/patients").header("Authorization", "Bearer " + token));
		var update = mockMvc.perform(patch("/api/v1/patients/" + patientId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Нове\" }"));
		var archive = mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive")
				.header("Authorization", "Bearer " + token));

		// Assert
		read.andExpect(status().isOk());
		list.andExpect(status().isOk());
		update.andExpect(status().isForbidden());
		archive.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("AD-19.3: PUT /patients/{id} — 405 METHOD_NOT_ALLOWED (змінюється лише частково через PATCH)")
	void putPatient_returns405() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Заміни", "+380501234652");
		String patientId = createPatient(mockMvc, ownerId, "Пацієнт Заміни", "Кіт");

		// Act
		var response = mockMvc.perform(put("/api/v1/patients/" + patientId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Повна Заміна\" }"));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("PATCH")));
	}
}
