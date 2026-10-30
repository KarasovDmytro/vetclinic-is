package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

/** REQ-F-004, REQ-F-005: доступ до керування обліковими записами лише в ролі ADMIN. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserAccessControlTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-005: лікар не може створити обліковий запис — 403")
	void veterinarianCreatesUser_returns403() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "kovalenko@clinic.example",
				  "fullName": "Коваленко Марія Ігорівна",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("vet").roles("VETERINARIAN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("AD-18: перевірка ролі йде перед валідацією тіла — лікар з невалідним запитом отримує 403")
	void veterinarianCreatesUserWithInvalidBody_returns403BeforeValidation() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "tkachenko@clinic.example",
				  "fullName": "Ткаченко Василь Іванович",
				  "role": "VETERINARIAN",
				  "password": "short"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("vet").roles("VETERINARIAN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-F-004: запит без автентифікації відхиляється — 401 UNAUTHORIZED")
	void anonymousCreatesUser_returns401Unauthorized() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "sydorenko@clinic.example",
				  "fullName": "Сидоренко Андрій Петрович",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-09: видалення облікових записів не підтримується — 405 METHOD_NOT_ALLOWED")
	void adminDeletesUser_returns405MethodNotAllowed() throws Exception {
		// Arrange
		String userId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

		// Act
		var response = mockMvc.perform(delete("/api/v1/users/" + userId)
				.with(user("admin").roles("ADMIN"))
				.with(csrf()));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code", equalTo("METHOD_NOT_ALLOWED")));
	}
}
