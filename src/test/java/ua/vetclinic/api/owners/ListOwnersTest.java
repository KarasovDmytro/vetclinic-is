package ua.vetclinic.api.owners;

import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-017, AD-15: пошук і список власників (операція listOwners). */
@ApiIntegrationTest
class ListOwnersTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-017: адміністратор отримує першу сторінку власників — 200 з page, pageSize, total, items")
	void adminListsOwners_returns200WithDefaultPage() throws Exception {
		// Arrange
		createOwner(mockMvc, "Списко Перший Іванович", "+380508889900");
		createOwner(mockMvc, "Списко Другий Іванович", "+380508889901");

		// Act
		var response = mockMvc.perform(get("/api/v1/owners")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)))
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.items[0].id").exists())
				.andExpect(jsonPath("$.items[0].fullName").exists());
	}

	@Test
	@DisplayName("AD-15: параметр q шукає підрядок у ПІБ без урахування регістру")
	void searchByQuery_returnsOwnersWithSubstringIgnoringCase() throws Exception {
		// Arrange
		createOwner(mockMvc, "Зінченко Юлія Андріївна", "+380509990011");
		createOwner(mockMvc, "Петренко Олег Іванович", "+380509990012");

		// Act
		var response = mockMvc.perform(get("/api/v1/owners")
				.param("q", "ІНЧЕН")
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)))
				.andExpect(jsonPath("$.items[*].fullName", hasItem("Зінченко Юлія Андріївна")))
				.andExpect(jsonPath("$.items[*].fullName", everyItem(containsStringIgnoringCase("інчен"))))
				.andExpect(jsonPath("$.items[*].fullName", not(hasItem("Петренко Олег Іванович"))));
	}

	@Test
	@DisplayName("AD-15: параметр q з одного символу — 422 VALIDATION_ERROR, поле q, причина TOO_SHORT")
	void searchWithOneCharacter_returns422TooShort() throws Exception {
		// Arrange
		String query = "а";

		// Act
		var response = mockMvc.perform(get("/api/v1/owners")
				.param("q", query)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("q")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}
}
