package ua.vetclinic.api.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-003, AD-01: вхід за email і паролем видає bearer-токен на 8 годин. */
@ApiIntegrationTest
class LoginTest {

	private static final String UUID_PATTERN =
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-01: вхід з вірними даними — 200, bearer-токен на 8 годин і дані користувача без пароля")
	void validCredentials_returns200WithBearerTokenAndUser() throws Exception {
		// Arrange
		createUser("kovalchuk@clinic.example", "Ковальчук Андрій Миколайович", "VETERINARIAN", "Vet-Passw0rd-2026");
		String loginBody = """
				{
				  "email": "kovalchuk@clinic.example",
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken", not(emptyString())))
				.andExpect(jsonPath("$.tokenType", equalTo("Bearer")))
				.andExpect(jsonPath("$.expiresIn", equalTo(28800)))
				.andExpect(jsonPath("$.user.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.user.email", equalTo("kovalchuk@clinic.example")))
				.andExpect(jsonPath("$.user.fullName", equalTo("Ковальчук Андрій Миколайович")))
				.andExpect(jsonPath("$.user.role", equalTo("VETERINARIAN")))
				.andExpect(jsonPath("$.user.password").doesNotExist())
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist());
	}

	private void createUser(String email, String fullName, String role, String password) throws Exception {
		String requestBody = """
				{
				  "email": "%s",
				  "fullName": "%s",
				  "role": "%s",
				  "password": "%s"
				}
				""".formatted(email, fullName, role, password);
		mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated());
	}
}
