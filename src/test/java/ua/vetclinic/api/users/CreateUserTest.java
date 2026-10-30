package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.TestcontainersConfiguration;

/** REQ-F-001: Адміністратор може створити обліковий запис із роллю Адміністратор або Лікар. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CreateUserTest {

	private static final String UUID_PATTERN =
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-001: адміністратор створює обліковий запис лікаря — 201 і відповідь без пароля")
	void adminCreatesVeterinarianAccount_returns201WithoutPassword() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "ivanenko@clinic.example",
				  "fullName": "Іваненко Олександр Петрович",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.email", equalTo("ivanenko@clinic.example")))
				.andExpect(jsonPath("$.fullName", equalTo("Іваненко Олександр Петрович")))
				.andExpect(jsonPath("$.role", equalTo("VETERINARIAN")))
				.andExpect(jsonPath("$.active", equalTo(true)))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.password").doesNotExist());
	}
}
