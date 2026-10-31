package ua.vetclinic.api.patients;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-012, REQ-F-014: Адміністратор реєструє пацієнта з прив'язкою до власника (операція createPatient). */
@ApiIntegrationTest
class CreatePatientTest {

	private static final String UUID_PATTERN =
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postPatient(String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("REQ-F-012: адміністратор реєструє пацієнта з усіма полями — 201, Location, статус ACTIVE від сервера")
	void adminCreatesPatient_returns201WithActiveStatus() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Барсика", "+380501234567");
		String requestBody = """
				{
				  "ownerId": "%s",
				  "name": "Барсик",
				  "species": "Кіт",
				  "breed": "Британська короткошерста",
				  "sex": "MALE",
				  "birthDate": "2021-03-15"
				}
				""".formatted(ownerId);

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/patients/" + UUID_PATTERN)))
				.andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.ownerId", equalTo(ownerId)))
				.andExpect(jsonPath("$.name", equalTo("Барсик")))
				.andExpect(jsonPath("$.species", equalTo("Кіт")))
				.andExpect(jsonPath("$.breed", equalTo("Британська короткошерста")))
				.andExpect(jsonPath("$.sex", equalTo("MALE")))
				.andExpect(jsonPath("$.birthDate", equalTo("2021-03-15")))
				.andExpect(jsonPath("$.status", equalTo("ACTIVE")))
				.andExpect(jsonPath("$.createdAt").exists())
				.andExpect(jsonPath("$.updatedAt").exists());
	}

	@Test
	@DisplayName("AD-10.2: лише обов'язкові поля — sex за замовчуванням UNKNOWN, breed і birthDate null")
	void minimalFields_applyDefaults() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Мінімуму", "+380501234568");
		String requestBody = """
				{ "ownerId": "%s", "name": "Рекс", "species": "Собака" }
				""".formatted(ownerId);

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sex", equalTo("UNKNOWN")))
				.andExpect(jsonPath("$.breed", nullValue()))
				.andExpect(jsonPath("$.birthDate", nullValue()))
				.andExpect(jsonPath("$.status", equalTo("ACTIVE")));
	}

	@Test
	@DisplayName("AD-18.3: неіснуючий власник у тілі — 422, поле ownerId, причина REFERENCE_NOT_FOUND (не 404)")
	void unknownOwner_returns422ReferenceNotFound() throws Exception {
		// Arrange
		String requestBody = """
				{ "ownerId": "00000000-0000-4000-8000-000000000501", "name": "Сирота", "species": "Кіт" }
				""";

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("ownerId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REFERENCE_NOT_FOUND")));
	}

	@Test
	@DisplayName("REQ-F-014: у одного власника може бути кілька пацієнтів, кожен прив'язаний рівно до одного власника")
	void ownerCanHaveSeveralPatients() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Двох Тварин", "+380501234569");
		String first = """
				{ "ownerId": "%s", "name": "Мурка", "species": "Кіт" }
				""".formatted(ownerId);
		String second = """
				{ "ownerId": "%s", "name": "Бобік", "species": "Собака" }
				""".formatted(ownerId);

		// Act
		var firstResponse = postPatient(first);
		var secondResponse = postPatient(second);

		// Assert
		firstResponse.andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId", equalTo(ownerId)));
		secondResponse.andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId", equalTo(ownerId)));
	}

	@Test
	@DisplayName("AD-10.3: статус задає лише сервер — поле status у запиті створення дає 422 UNKNOWN_FIELD")
	void clientCannotSetStatus_returns422() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Статусу", "+380501234570");
		String requestBody = """
				{ "ownerId": "%s", "name": "Статус", "species": "Кіт", "status": "ARCHIVED" }
				""".formatted(ownerId);

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("status")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може реєструвати пацієнтів — 403 FORBIDDEN")
	void veterinarianCreatesPatient_returns403() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Лікаря", "+380501234571");
		String requestBody = """
				{ "ownerId": "%s", "name": "Заборонено", "species": "Кіт" }
				""".formatted(ownerId);

		// Act
		var response = mockMvc.perform(post("/api/v1/patients")
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}
}
