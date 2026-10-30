package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ua.vetclinic.api.TestcontainersConfiguration;

/** REQ-F-002: Адміністратор може деактивувати обліковий запис (операція deactivateUser). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class DeactivateUserTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-002: адміністратор деактивує лікаря — 200 і active=false")
	void adminDeactivatesVeterinarian_returns200WithActiveFalse() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "hrytsenko@clinic.example",
				  "fullName": "Гриценко Оксана Миколаївна",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";
		MvcResult created = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		String userId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Act
		var response = mockMvc.perform(post("/api/v1/users/" + userId + "/deactivate")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(userId)))
				.andExpect(jsonPath("$.active", equalTo(false)));
	}
}
