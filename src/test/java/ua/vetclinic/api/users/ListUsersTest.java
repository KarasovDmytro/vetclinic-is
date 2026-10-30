package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

/** REQ-F-001: Адміністратор переглядає список облікових записів (операція listUsers). */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ListUsersTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-001: адміністратор отримує першу сторінку користувачів — 200 з page, pageSize, total, items")
	void adminListsUsers_returns200WithDefaultPage() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "kravchenko@clinic.example",
				  "fullName": "Кравченко Юрій Андрійович",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";
		mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated());

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)))
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.items.length()", greaterThan(0)))
				.andExpect(jsonPath("$.items[0].password").doesNotExist());
	}
}
