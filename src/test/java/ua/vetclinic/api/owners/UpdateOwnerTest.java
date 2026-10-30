package ua.vetclinic.api.owners;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-011: Адміністратор редагує дані власника (операція updateOwner). */
@ApiIntegrationTest
class UpdateOwnerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-011: адміністратор змінює ПІБ — 200, ПІБ оновлено, решта полів без змін")
	void adminUpdatesFullName_returns200AndKeepsOtherFields() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Гончаренко Віра Миколаївна", "+380503334455");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Гончаренко-Мазур Віра Миколаївна" }
						"""));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(ownerId)))
				.andExpect(jsonPath("$.fullName", equalTo("Гончаренко-Мазур Віра Миколаївна")))
				.andExpect(jsonPath("$.phone", equalTo("+380503334455")))
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("REQ-F-011: email: null очищає необов'язкове поле (AD-17)")
	void nullEmail_clearsOptionalField() throws Exception {
		// Arrange
		var created = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Очищенко Євген Петрович",
						  "phone": "+380504445566",
						  "email": "clear.me@example.com"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownerId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": null }
						"""));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.email", nullValue()))
				.andExpect(jsonPath("$.fullName", equalTo("Очищенко Євген Петрович")));
	}

	@Test
	@DisplayName("REQ-F-011: редагування неіснуючого власника — 404 NOT_FOUND")
	void updateUnknownOwner_returns404NotFound() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000411";

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + unknownId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Невідомий Власник" }
						"""));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("REQ-F-011: порожнє тіло {} — 422 VALIDATION_ERROR з причиною EMPTY_BODY (minProperties: 1)")
	void emptyBody_returns422EmptyBody() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Порожній Запит Іванович", "+380505556677");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}"));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("EMPTY_BODY")));
	}

	@Test
	@DisplayName("REQ-F-011: телефон не за шаблоном — 422, поле phone, причина INVALID_FORMAT (AD-17)")
	void invalidPhone_returns422InvalidFormat() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Телефонний Номер Петрович", "+380506667788");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "phone": "050-666-77-88" }
						"""));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може редагувати власників — 403 FORBIDDEN")
	void veterinarianUpdatesOwner_returns403Forbidden() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Заборонений Доступ Іванович", "+380507778899");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Лікарська Правка" }
						"""));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}
}
