package ua.vetclinic.api.owners;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-004, REQ-NF-002, AD-18, AD-19: доступ за ролями, порядок перевірок і конвенції API для власників. */
@ApiIntegrationTest
class OwnerAccessAndContractTest {

	private static final String CLINIC_OFFSET_TIMESTAMP =
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+0[23]:00";

	private static final String UNKNOWN_ID = "00000000-0000-4000-8000-000000000499";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-19.1: createdAt і updatedAt — ISO 8601 із зсувом часового поясу клініки, при створенні вони рівні")
	void timestamps_areIsoWithClinicOffsetAndEqualOnCreation() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Часовий Пояс Петрович", "phone": "+380501234567" }
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		var result = response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andReturn();
		String body = result.getResponse().getContentAsString();
		String createdAt = JsonPath.read(body, "$.createdAt");
		String updatedAt = JsonPath.read(body, "$.updatedAt");
		org.hamcrest.MatcherAssert.assertThat(updatedAt, equalTo(createdAt));
	}

	@Test
	@DisplayName("REQ-F-010: збережений власник повертається через GET з усіма полями, часові мітки у часовому поясі клініки")
	void createdOwner_isReturnedByGetWithAllFields() throws Exception {
		// Arrange
		var created = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Повний Запис Іванович",
						  "phone": "+380501234567",
						  "email": "full.record@example.com",
						  "address": "м. Харків, вул. Сумська, 10"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownerId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Act
		var response = mockMvc.perform(get("/api/v1/owners/" + ownerId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName", equalTo("Повний Запис Іванович")))
				.andExpect(jsonPath("$.phone", equalTo("+380501234567")))
				.andExpect(jsonPath("$.email", equalTo("full.record@example.com")))
				.andExpect(jsonPath("$.address", equalTo("м. Харків, вул. Сумська, 10")))
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)));
	}

	@Test
	@DisplayName("REQ-NF-002: POST /owners без токена — 401 UNAUTHORIZED із WWW-Authenticate")
	void createOwnerWithoutToken_returns401() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Анонім Без Токена", "phone": "+380501234567" }
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", equalTo("Bearer")))
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /owners без токена — 401 UNAUTHORIZED")
	void listOwnersWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/owners");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /owners/{id} без токена — 401 UNAUTHORIZED")
	void getOwnerWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/owners/" + UNKNOWN_ID);

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-NF-002: PATCH /owners/{id} без токена — 401 UNAUTHORIZED")
	void updateOwnerWithoutToken_returns401() throws Exception {
		// Arrange
		var request = patch("/api/v1/owners/" + UNKNOWN_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Без Токена" }
						""");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-F-004: справжній токен лікаря, виданий при вході, дозволяє читати власників, але не створювати")
	void veterinarianToken_readsButCannotCreateOwners() throws Exception {
		// Arrange
		createUser(mockMvc, "owners.vet@clinic.example", "Лікар Читач Петрович", "VETERINARIAN", "Vet-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "owners.vet@clinic.example", "Vet-Passw0rd-2026");

		// Act
		var listResponse = mockMvc.perform(get("/api/v1/owners")
				.header("Authorization", "Bearer " + token));
		var createResponse = mockMvc.perform(post("/api/v1/owners")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Заборонено Лікарю", "phone": "+380501234567" }
						"""));

		// Assert
		listResponse.andExpect(status().isOk());
		createResponse.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — лікар на неіснуючому власнику отримує 403, а не 404")
	void veterinarianUpdatesUnknownOwner_returns403Before404() throws Exception {
		// Arrange
		var request = patch("/api/v1/owners/" + UNKNOWN_ID)
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Перевірка Порядку" }
						""");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — невалідне тіло для неіснуючого власника дає 422, а не 404")
	void invalidBodyForUnknownOwner_returns422Before404() throws Exception {
		// Arrange
		var request = patch("/api/v1/owners/" + UNKNOWN_ID)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "phone": "12345" }
						""");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("AD-18: лікар запитує неіснуючого власника — 404 NOT_FOUND (читання йому дозволене)")
	void veterinarianGetsUnknownOwner_returns404() throws Exception {
		// Arrange
		var request = get("/api/v1/owners/" + UNKNOWN_ID)
				.with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор власника не UUID — 400 MALFORMED_REQUEST для GET")
	void getOwnerWithInvalidId_returns400() throws Exception {
		// Arrange
		var request = get("/api/v1/owners/not-a-uuid")
				.with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор власника не UUID — 400 MALFORMED_REQUEST для PATCH")
	void updateOwnerWithInvalidId_returns400() throws Exception {
		// Arrange
		var request = patch("/api/v1/owners/not-a-uuid")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Невірний Ідентифікатор" }
						""");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-09, AD-18.2: DELETE /owners/{id} — 405 METHOD_NOT_ALLOWED із заголовком Allow, видалення немає")
	void deleteOwner_returns405WithAllowHeader() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Не Видаляти Петрович", "+380501234567");

		// Act
		var response = mockMvc.perform(delete("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code", equalTo("METHOD_NOT_ALLOWED")))
				.andExpect(header().string("Allow", containsString("GET")))
				.andExpect(header().string("Allow", containsString("PATCH")));
		mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-19.3: PUT /owners/{id} — 405 METHOD_NOT_ALLOWED (змінюється лише частково через PATCH)")
	void putOwner_returns405() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Лише Патч Петрович", "+380501234567");

		// Act
		var response = mockMvc.perform(put("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Повна Заміна", "phone": "+380501234567" }
						"""));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code", equalTo("METHOD_NOT_ALLOWED")));
	}
}
