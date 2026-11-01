package ua.vetclinic.api.owners;

import static org.hamcrest.Matchers.equalTo;
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

/** REQ-F-010: перегляд власника (операція getOwner); читання доступне адміністратору й лікарю (AD-10.4, AD-06). */
@ApiIntegrationTest
class GetOwnerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-010: адміністратор отримує власника за id — 200 з даними власника")
	void adminGetsOwner_returns200WithOwner() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Шевченко Тарас Григорович", "+380501112233");

		// Act
		var response = mockMvc.perform(get("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(ownerId)))
				.andExpect(jsonPath("$.fullName", equalTo("Шевченко Тарас Григорович")))
				.andExpect(jsonPath("$.phone", equalTo("+380501112233")));
	}

	@Test
	@DisplayName("AD-06: лікар переглядає власника — 200 (читання дозволене)")
	void veterinarianGetsOwner_returns200() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Українка Леся Петрівна", "+380502223344");

		// Act
		var response = mockMvc.perform(get("/api/v1/owners/" + ownerId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(ownerId)));
	}

	@Test
	@DisplayName("REQ-F-010: неіснуючий власник — 404 NOT_FOUND")
	void unknownOwner_returns404NotFound() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000404";

		// Act
		var response = mockMvc.perform(get("/api/v1/owners/" + unknownId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}
}
