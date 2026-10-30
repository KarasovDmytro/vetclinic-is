package ua.vetclinic.api.auth;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
		createUser("bearer.admin@clinic.example", "Адміненко Віра Степанівна", "ADMIN", "Admin-Passw0rd-2026");
		String token = loginAndGetToken("bearer.admin@clinic.example", "Admin-Passw0rd-2026");

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.header("Authorization", "Bearer " + token));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)));
	}

	private String createUser(String email, String fullName, String role, String password) throws Exception {
		String requestBody = """
				{
				  "email": "%s",
				  "fullName": "%s",
				  "role": "%s",
				  "password": "%s"
				}
				""".formatted(email, fullName, role, password);
		var created = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	private String loginAndGetToken(String email, String password) throws Exception {
		String loginBody = """
				{
				  "email": "%s",
				  "password": "%s"
				}
				""".formatted(email, password);
		var login = mockMvc.perform(post("/api/v1/auth/login")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
	}
}
