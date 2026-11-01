package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
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
import org.springframework.test.web.servlet.MvcResult;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-001: створений обліковий запис можна отримати за ідентифікатором (операція getUser). */
@ApiIntegrationTest
class GetUserTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-001: адміністратор отримує створеного користувача за адресою з Location — 200 без пароля")
	void adminGetsCreatedUserByLocation_returns200WithoutPassword() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "petrenko@clinic.example",
				  "fullName": "Петренко Ірина Василівна",
				  "role": "ADMIN",
				  "password": "Admin-Passw0rd-2026"
				}
				""";
		MvcResult created = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		String location = created.getResponse().getHeader("Location");
		String createdId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Act
		var response = mockMvc.perform(get(location)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(createdId)))
				.andExpect(jsonPath("$.email", equalTo("petrenko@clinic.example")))
				.andExpect(jsonPath("$.fullName", equalTo("Петренко Ірина Василівна")))
				.andExpect(jsonPath("$.role", equalTo("ADMIN")))
				.andExpect(jsonPath("$.active", equalTo(true)))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	@Test
	@DisplayName("REQ-F-005: лікар не може отримати обліковий запис — 403")
	void veterinarianGetsUser_returns403() throws Exception {
		// Arrange
		String userId = "7c9e6679-7425-40de-944b-e07fc1f90ae7";

		// Act
		var response = mockMvc.perform(get("/api/v1/users/" + userId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-F-001: користувача з таким id немає — 404 NOT_FOUND")
	void adminGetsUnknownUser_returns404NotFound() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000404";

		// Act
		var response = mockMvc.perform(get("/api/v1/users/" + unknownId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-19.1: createdAt користувача віддається з зсувом часового поясу клініки, а не в UTC")
	void createdAt_isReturnedInClinicTimeZone() throws Exception {
		// Arrange
		String userId = ua.vetclinic.api.TestAccounts.createUser(mockMvc, "zone.user@clinic.example",
				"Часовий Пояс Користувач", "VETERINARIAN");

		// Act
		var response = mockMvc.perform(get("/api/v1/users/" + userId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.createdAt",
						matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?\\+0[23]:00")));
	}
}
