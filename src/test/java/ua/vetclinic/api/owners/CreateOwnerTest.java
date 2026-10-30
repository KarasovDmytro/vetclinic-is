package ua.vetclinic.api.owners;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-010: Адміністратор реєструє нового власника (операція createOwner). */
@ApiIntegrationTest
class CreateOwnerTest {

	private static final String UUID_PATTERN =
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-010: адміністратор реєструє власника з усіма полями — 201, Location і дані власника")
	void adminCreatesOwner_returns201WithOwner() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "Коваленко Марія Іванівна",
				  "phone": "+380501234567",
				  "email": "Kovalenko@Example.com",
				  "address": "м. Київ, вул. Хрещатик, 1"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/owners/" + UUID_PATTERN)))
				.andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.fullName", equalTo("Коваленко Марія Іванівна")))
				.andExpect(jsonPath("$.phone", equalTo("+380501234567")))
				.andExpect(jsonPath("$.email", equalTo("Kovalenko@Example.com")))
				.andExpect(jsonPath("$.address", equalTo("м. Київ, вул. Хрещатик, 1")))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("REQ-F-010: email та адреса необов'язкові — 201, у відповіді вони null (AD-10)")
	void adminCreatesOwnerWithoutOptionalFields_returns201WithNulls() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "Бойко Петро Степанович",
				  "phone": "0671234567"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName", equalTo("Бойко Петро Степанович")))
				.andExpect(jsonPath("$.email", nullValue()))
				.andExpect(jsonPath("$.address", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-010: ПІБ не вказано — 422 VALIDATION_ERROR, поле fullName, причина REQUIRED (AD-17)")
	void missingFullName_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "phone": "+380501234567"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("REQ-F-010: телефон не за шаблоном (пробіли, дужки) — 422, поле phone, причина INVALID_FORMAT (AD-17)")
	void phoneWithSpaces_returns422InvalidFormat() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "Мельник Ольга Василівна",
				  "phone": "+38 (050) 123-45-67"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("REQ-F-010: ПІБ довший за 150 символів — 422, поле fullName, причина TOO_LONG (AD-17)")
	void tooLongFullName_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "%s",
				  "phone": "+380501234567"
				}
				""".formatted("Ш".repeat(151));

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може реєструвати власників — 403 FORBIDDEN")
	void veterinarianCreatesOwner_returns403Forbidden() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "Лікарів Спроба Петрович",
				  "phone": "+380501234567"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/owners")
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}
}
