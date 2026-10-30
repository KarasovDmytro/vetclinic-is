package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-01.5: перший адміністратор створюється при розгортанні, а не через API. */
@ApiIntegrationTest
@TestPropertySource(properties = {
		"vetclinic.bootstrap.admin-email=initial.admin@clinic.example",
		"vetclinic.bootstrap.admin-password=Initial-Passw0rd-2026"
})
class InitialAdminSeedTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-01.5: початковий адміністратор із налаштувань розгортання входить у систему — 200 і роль ADMIN")
	void configuredInitialAdmin_canLogIn() throws Exception {
		// Arrange
		String loginBody = """
				{
				  "email": "initial.admin@clinic.example",
				  "password": "Initial-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(loginBody));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.user.email", equalTo("initial.admin@clinic.example")))
				.andExpect(jsonPath("$.user.role", equalTo("ADMIN")));
	}
}
