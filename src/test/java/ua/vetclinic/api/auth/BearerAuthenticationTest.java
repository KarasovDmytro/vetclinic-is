package ua.vetclinic.api.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-01: виданий bearer-токен автентифікує подальші запити. */
@ApiIntegrationTest
class BearerAuthenticationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-01: токен адміністратора, виданий при вході, відкриває доступ до списку користувачів")
	void adminToken_allowsListingUsers() throws Exception {
		// Arrange
		createUser(mockMvc, "bearer.admin@clinic.example", "Адміненко Віра Степанівна", "ADMIN", "Admin-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "bearer.admin@clinic.example", "Admin-Passw0rd-2026");

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + token));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)));
	}

	@Test
	@DisplayName("AD-01: токен деактивованого користувача перестає діяти одразу — 401 UNAUTHORIZED")
	void tokenOfDeactivatedUser_returns401Unauthorized() throws Exception {
		// Arrange
		String userId = createUser(mockMvc, "bearer.deactivated@clinic.example", "Деактивенко Ігор Романович", "ADMIN",
				"Admin-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "bearer.deactivated@clinic.example", "Admin-Passw0rd-2026");
		mockMvc.perform(post("/api/v1/users/" + userId + "/deactivate")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()))
				.andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + token));

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-01: підроблений токен — 401 UNAUTHORIZED із заголовком WWW-Authenticate: Bearer")
	void tamperedToken_returns401UnauthorizedWithBearerChallenge() throws Exception {
		// Arrange
		String forgedToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJmb3JnZWQiLCJyb2xlIjoiQURNSU4ifQ.forged-signature";

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + forgedToken));

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", equalTo("Bearer")))
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")))
				.andExpect(jsonPath("$.message", equalTo("Потрібна автентифікація")));
	}

	@Test
	@DisplayName("AD-01: безстанове API — створення користувача за bearer-токеном без CSRF-токена — 201")
	void adminToken_createsUserWithoutCsrfToken() throws Exception {
		// Arrange
		createUser(mockMvc, "bearer.csrf.admin@clinic.example", "Безстан Орест Ігорович", "ADMIN", "Admin-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "bearer.csrf.admin@clinic.example", "Admin-Passw0rd-2026");
		String requestBody = """
				{
				  "email": "bearer.csrf.vet@clinic.example",
				  "fullName": "Ветеринар Оксана Ігорівна",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isCreated());
	}
}
