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

	@Test
	@DisplayName("REQ-F-001: параметр pageSize обмежує розмір сторінки")
	void adminListsUsersWithPageSize_returnsRequestedPageSize() throws Exception {
		// Arrange
		createUser("pavlenko@clinic.example", "Павленко Ірина Олексіївна", "VETERINARIAN");
		createUser("rudenko@clinic.example", "Руденко Максим Сергійович", "VETERINARIAN");

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.param("pageSize", "1")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.pageSize", equalTo(1)))
				.andExpect(jsonPath("$.items.length()", equalTo(1)))
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(2)));
	}

	@Test
	@DisplayName("REQ-F-001: параметр page повертає вказану сторінку")
	void adminListsUsersWithPage_returnsRequestedPage() throws Exception {
		// Arrange
		createUser("savchenko@clinic.example", "Савченко Оксана Петрівна", "VETERINARIAN");
		createUser("tarasenko@clinic.example", "Тарасенко Богдан Ігорович", "VETERINARIAN");

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.param("page", "2")
				.param("pageSize", "1")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(2)))
				.andExpect(jsonPath("$.pageSize", equalTo(1)))
				.andExpect(jsonPath("$.items.length()", equalTo(1)));
	}

	@Test
	@DisplayName("AD-17: pageSize більший за 100 — 422 VALIDATION_ERROR з причиною OUT_OF_RANGE")
	void adminListsUsersWithTooLargePageSize_returns422OutOfRange() throws Exception {
		// Arrange
		String pageSize = "101";

		// Act
		var response = mockMvc.perform(get("/api/v1/users")
				.param("pageSize", pageSize)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("pageSize")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	private void createUser(String email, String fullName, String role) throws Exception {
		String requestBody = """
				{
				  "email": "%s",
				  "fullName": "%s",
				  "role": "%s",
				  "password": "Vet-Passw0rd-2026"
				}
				""".formatted(email, fullName, role);
		mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated());
	}
}
