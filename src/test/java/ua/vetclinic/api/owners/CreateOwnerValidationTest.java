package ua.vetclinic.api.owners;

import static ua.vetclinic.api.TestData.LONG_EMAIL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-010, AD-17, AD-18: правила полів власника при реєстрації (операція createOwner). */
@ApiIntegrationTest
class CreateOwnerValidationTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postOwner(String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("AD-17: email без формату адреси — 422, поле email, причина INVALID_FORMAT")
	void invalidEmail_returns422InvalidFormat() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+380501234567", "email": "not-an-email" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("AD-17: email довший за 254 символи — 422, поле email, причина TOO_LONG")
	void tooLongEmail_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+380501234567", "email": "%s" }
				""".formatted(LONG_EMAIL);

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17: порожній email замість null — 422, поле email, причина INVALID_FORMAT")
	void emptyEmail_returns422InvalidFormat() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+380501234567", "email": "" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("AD-17: адреса довша за 300 символів — 422, поле address, причина TOO_LONG")
	void tooLongAddress_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+380501234567", "address": "%s" }
				""".formatted("а".repeat(301));

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("address")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17: порожня адреса замість null — 422, поле address, причина TOO_SHORT")
	void emptyAddress_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+380501234567", "address": "" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("address")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17: телефон не вказано — 422, поле phone, причина REQUIRED")
	void missingPhone_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17: телефон із 9 цифр — 422, поле phone, причина INVALID_FORMAT")
	void phoneWithNineDigits_returns422InvalidFormat() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "050123456" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("AD-17: телефон із 16 цифр — 422, поле phone, причина INVALID_FORMAT")
	void phoneWithSixteenDigits_returns422InvalidFormat() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "0501234567890123" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("AD-17: телефон із літерами — 422, поле phone, причина INVALID_FORMAT")
	void phoneWithLetters_returns422InvalidFormat() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+38050abc4567" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("AD-17: ПІБ з самих пробілів — 422, поле fullName, причина REQUIRED")
	void blankFullName_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "   ", "phone": "+380501234567" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17: ПІБ явно null — 422, поле fullName, причина REQUIRED")
	void nullFullName_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": null, "phone": "+380501234567" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17: ПІБ рівно з 150 символів і телефон «+» та 15 цифр — межа допустима, 201")
	void valuesOnUpperBounds_return201() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "%s", "phone": "+380501234567890", "address": "%s" }
				""".formatted("Ш".repeat(150), "а".repeat(300));

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.phone", equalTo("+380501234567890")));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях рядків обрізаються")
	void surroundingWhitespace_isTrimmed() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "  Обрізаний Власник  ",
				  "phone": " +380501234567 ",
				  "email": " trimmed@example.com ",
				  "address": "  м. Одеса  "
				}
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.fullName", equalTo("Обрізаний Власник")))
				.andExpect(jsonPath("$.phone", equalTo("+380501234567")))
				.andExpect(jsonPath("$.email", equalTo("trimmed@example.com")))
				.andExpect(jsonPath("$.address", equalTo("м. Одеса")));
	}

	@Test
	@DisplayName("AD-18: кілька помилок полів повертаються разом у details")
	void severalInvalidFields_areAllReported() throws Exception {
		// Arrange
		String requestBody = """
				{ "phone": "12345", "email": "not-an-email" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(3)));
	}

	@Test
	@DisplayName("AD-18: невідоме поле в тілі — 422, причина UNKNOWN_FIELD")
	void unknownField_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = """
				{ "fullName": "Валідний Власник", "phone": "+380501234567", "nickname": "Бос" }
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("nickname")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: поле id формується сервером і в запиті недопустиме — 422 UNKNOWN_FIELD")
	void serverManagedId_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "id": "00000000-0000-4000-8000-000000000001",
				  "fullName": "Валідний Власник",
				  "phone": "+380501234567"
				}
				""";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("id")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18: зламаний JSON — 400 MALFORMED_REQUEST")
	void malformedJson_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"fullName\": ";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: тіло — масив замість об'єкта — 400 MALFORMED_REQUEST")
	void arrayBody_returns400() throws Exception {
		// Arrange
		String requestBody = "[]";

		// Act
		var response = postOwner(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: запит без тіла — 400 MALFORMED_REQUEST")
	void missingBody_returns400() throws Exception {
		// Arrange
		var request = post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON);

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: тіло не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void plainTextBody_returns415() throws Exception {
		// Arrange
		var request = post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("fullName=Власник");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}
}
