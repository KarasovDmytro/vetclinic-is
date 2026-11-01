package ua.vetclinic.api.patients;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;
import static ua.vetclinic.api.TestPatients.createPatient;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-012, REQ-F-016: перегляд пацієнта (операція getPatient); читання доступне адміністратору й лікарю. */
@ApiIntegrationTest
class GetPatientTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("REQ-F-012: збережений пацієнт повертається через GET з усіма полями і статусом ACTIVE")
	void adminGetsCreatedPatient_returnsAllFields() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Читання", "+380501234590");
		var created = mockMvc.perform(post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "ownerId": "%s",
						  "name": "Мурзик",
						  "species": "Кіт",
						  "breed": "Сіамська",
						  "sex": "FEMALE",
						  "birthDate": "2020-05-01"
						}
						""".formatted(ownerId)))
				.andExpect(status().isCreated())
				.andReturn();
		String patientId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		// Act
		var response = mockMvc.perform(get("/api/v1/patients/" + patientId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(patientId)))
				.andExpect(jsonPath("$.ownerId", equalTo(ownerId)))
				.andExpect(jsonPath("$.name", equalTo("Мурзик")))
				.andExpect(jsonPath("$.species", equalTo("Кіт")))
				.andExpect(jsonPath("$.breed", equalTo("Сіамська")))
				.andExpect(jsonPath("$.sex", equalTo("FEMALE")))
				.andExpect(jsonPath("$.birthDate", equalTo("2020-05-01")))
				.andExpect(jsonPath("$.status", equalTo("ACTIVE")));
	}

	@Test
	@DisplayName("AD-10.4: лікар переглядає пацієнта — 200, у відповіді немає діагнозів і призначень")
	void veterinarianGetsPatient_returns200WithoutMedicalData() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Для Лікаря", "+380501234591");
		String patientId = createPatient(mockMvc, ownerId, "Пацієнт Лікаря", "Собака");

		// Act
		var response = mockMvc.perform(get("/api/v1/patients/" + patientId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(patientId)))
				.andExpect(jsonPath("$.breed", nullValue()))
				.andExpect(jsonPath("$.diagnosis").doesNotExist())
				.andExpect(jsonPath("$.prescriptions").doesNotExist());
	}

	@Test
	@DisplayName("REQ-F-012: неіснуючий пацієнт — 404 NOT_FOUND")
	void unknownPatient_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000504";

		// Act
		var response = mockMvc.perform(get("/api/v1/patients/" + unknownId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор пацієнта не UUID — 400 MALFORMED_REQUEST")
	void patientIdNotUuid_returns400() throws Exception {
		// Arrange
		var request = get("/api/v1/patients/not-a-uuid").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /patients/{id} без токена — 401 UNAUTHORIZED")
	void getPatientWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/patients/00000000-0000-4000-8000-000000000504");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}
}
