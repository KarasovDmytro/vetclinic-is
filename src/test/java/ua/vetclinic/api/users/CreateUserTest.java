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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-001: Адміністратор може створити обліковий запис із роллю Адміністратор або Лікар. */
@ApiIntegrationTest
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

	@Test
	@DisplayName("REQ-F-001: email уже використано — 409 EMAIL_TAKEN")
	void adminCreatesUserWithTakenEmail_returns409EmailTaken() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "bondarenko@clinic.example",
				  "fullName": "Бондаренко Наталія Сергіївна",
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
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("EMAIL_TAKEN")));
	}

	@Test
	@DisplayName("REQ-F-001: email у відповіді зберігається в нижньому регістрі (AD-17)")
	void adminCreatesUserWithMixedCaseEmail_returnsLowercaseEmail() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "Melnyk.Olena@Clinic.Example",
				  "fullName": "Мельник Олена Дмитрівна",
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
				.andExpect(jsonPath("$.email", equalTo("melnyk.olena@clinic.example")));
	}

	@Test
	@DisplayName("REQ-F-001: пароль коротший за 8 символів — 422 VALIDATION_ERROR (AD-11)")
	void adminCreatesUserWithTooShortPassword_returns422ValidationError() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "shevchenko@clinic.example",
				  "fullName": "Шевченко Тарас Григорович",
				  "role": "VETERINARIAN",
				  "password": "Short7!"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("REQ-F-001: email без формату адреси — 422 VALIDATION_ERROR (AD-17)")
	void adminCreatesUserWithInvalidEmail_returns422ValidationError() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "not-an-email",
				  "fullName": "Гончар Ігор Романович",
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
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("REQ-F-001: ПІБ не вказано — 422 VALIDATION_ERROR (AD-17)")
	void adminCreatesUserWithoutFullName_returns422ValidationError() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "oliinyk@clinic.example",
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
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")));
	}

	@Test
	@DisplayName("REQ-F-001: email не вказано — 422 VALIDATION_ERROR, поле email, причина REQUIRED (AD-17)")
	void adminCreatesUserWithoutEmail_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "fullName": "Романюк Ольга Степанівна",
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
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("REQ-F-001: роль не вказано — 422 VALIDATION_ERROR, поле role, причина REQUIRED (AD-17)")
	void adminCreatesUserWithoutRole_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "kuzmenko@clinic.example",
				  "fullName": "Кузьменко Леся Андріївна",
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
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("role")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("REQ-F-001: пароль не вказано — 422 VALIDATION_ERROR, поле password, причина REQUIRED (AD-11)")
	void adminCreatesUserWithoutPassword_returns422Required() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "hrytsenko@clinic.example",
				  "fullName": "Гриценко Павло Васильович",
				  "role": "VETERINARIAN"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("password")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("REQ-F-001: пароль коротший за 8 символів — поле password, причина TOO_SHORT (AD-11)")
	void adminCreatesUserWithTooShortPassword_returnsTooShortReason() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "stepanenko@clinic.example",
				  "fullName": "Степаненко Віра Миколаївна",
				  "role": "VETERINARIAN",
				  "password": "Short7!"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("password")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("REQ-F-001: пароль довший за 72 символи — поле password, причина TOO_LONG (AD-11)")
	void adminCreatesUserWithTooLongPassword_returnsTooLongReason() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "boiko@clinic.example",
				  "fullName": "Бойко Михайло Тарасович",
				  "role": "VETERINARIAN",
				  "password": "%s"
				}
				""".formatted("p".repeat(73));

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("password")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("REQ-F-001: ПІБ довший за 150 символів — поле fullName, причина TOO_LONG (AD-17)")
	void adminCreatesUserWithTooLongFullName_returnsTooLongReason() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "dovzhenko@clinic.example",
				  "fullName": "%s",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""".formatted("Я".repeat(151));

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}
}
