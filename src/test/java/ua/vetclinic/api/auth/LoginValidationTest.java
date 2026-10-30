package ua.vetclinic.api.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** openapi.yaml, LoginRequest: email ≤254 у форматі адреси, password 1–72. */
@ApiIntegrationTest
class LoginValidationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-18: email не вказано — 422 VALIDATION_ERROR, поле email, причина REQUIRED")
	void missingEmail_returns422Required() throws Exception {
		// Arrange
		String loginBody = """
				{
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-18: пароль не вказано — 422 VALIDATION_ERROR, поле password, причина REQUIRED")
	void missingPassword_returns422Required() throws Exception {
		// Arrange
		String loginBody = """
				{
				  "email": "login.nopassword@clinic.example"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("password")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}
}
