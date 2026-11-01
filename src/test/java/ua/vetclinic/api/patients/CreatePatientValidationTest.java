package ua.vetclinic.api.patients;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;

import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-012, AD-10, AD-17.4, AD-18: правила полів пацієнта при реєстрації (операція createPatient). */
@ApiIntegrationTest
class CreatePatientValidationTest {

	@Autowired
	private MockMvc mockMvc;

	private String ownerId;

	@BeforeEach
	void createOwnerForPatients() throws Exception {
		ownerId = createOwner(mockMvc, "Власник Валідації", "+380501234580");
	}

	private ResultActions postPatient(String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	private String body(String fieldsAfterOwner) {
		return "{ \"ownerId\": \"" + ownerId + "\", " + fieldsAfterOwner + " }";
	}

	@Test
	@DisplayName("AD-17.4: ім'я не вказано — 422, поле name, причина REQUIRED")
	void missingName_returns422Required() throws Exception {
		// Arrange
		String requestBody = body("\"species\": \"Кіт\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.4: ім'я з самих пробілів — 422, поле name, причина REQUIRED")
	void blankName_returns422Required() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"   \", \"species\": \"Кіт\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.4: ім'я довше за 100 символів — 422, поле name, причина TOO_LONG")
	void tooLongName_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"" + "Б".repeat(101) + "\", \"species\": \"Кіт\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.4: межі допустимі — ім'я 100, вид 50, порода 100 символів, дата народження сьогодні — 201")
	void valuesOnBounds_return201() throws Exception {
		// Arrange
		String today = LocalDate.now(ZoneId.of("Europe/Kyiv")).toString();
		String requestBody = body("\"name\": \"" + "Б".repeat(100) + "\", \"species\": \"" + "В".repeat(50)
				+ "\", \"breed\": \"" + "П".repeat(100) + "\", \"birthDate\": \"" + today + "\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.birthDate", equalTo(today)));
	}

	@Test
	@DisplayName("AD-17.4: вид не вказано — 422, поле species, причина REQUIRED")
	void missingSpecies_returns422Required() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("species")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.4: вид довший за 50 символів — 422, поле species, причина TOO_LONG")
	void tooLongSpecies_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"" + "В".repeat(51) + "\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("species")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.4: порода довша за 100 символів — 422, поле breed, причина TOO_LONG")
	void tooLongBreed_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"Кіт\", \"breed\": \"" + "П".repeat(101) + "\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("breed")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.1: порожня порода замість null — 422, поле breed, причина TOO_SHORT")
	void emptyBreed_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"Кіт\", \"breed\": \"\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("breed")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-10.2: ownerId не вказано — 422, поле ownerId, причина REQUIRED")
	void missingOwnerId_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"Барсик\", \"species\": \"Кіт\" }";

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("ownerId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-18.2: ownerId не UUID — 400 MALFORMED_REQUEST")
	void ownerIdNotUuid_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"ownerId\": \"not-a-uuid\", \"name\": \"Барсик\", \"species\": \"Кіт\" }";

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: невідоме значення sex — 422, поле sex, причина INVALID_ENUM")
	void invalidSex_returns422InvalidEnum() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"Кіт\", \"sex\": \"UNKNOWN_SEX\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("sex")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_ENUM")));
	}

	@Test
	@DisplayName("AD-17.4: дата народження в майбутньому — 422, поле birthDate, причина OUT_OF_RANGE")
	void futureBirthDate_returns422OutOfRange() throws Exception {
		// Arrange
		String tomorrow = LocalDate.now(ZoneId.of("Europe/Kyiv")).plusDays(1).toString();
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"Кіт\", \"birthDate\": \"" + tomorrow + "\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("birthDate")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-18.2: дата народження не у форматі YYYY-MM-DD — 400 MALFORMED_REQUEST")
	void birthDateWithWrongFormat_returns400() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"Кіт\", \"birthDate\": \"15-03-2021\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях імені, виду та породи обрізаються")
	void surroundingWhitespace_isTrimmed() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"  Барсик  \", \"species\": \" Кіт \", \"breed\": \"  Мейн-кун \"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name", equalTo("Барсик")))
				.andExpect(jsonPath("$.species", equalTo("Кіт")))
				.andExpect(jsonPath("$.breed", equalTo("Мейн-кун")));
	}

	@Test
	@DisplayName("AD-18: кілька помилок полів повертаються разом у details")
	void severalInvalidFields_areAllReported() throws Exception {
		// Arrange
		String requestBody = "{ \"sex\": \"MALE\" }";

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(3)));
	}

	@Test
	@DisplayName("AD-18: невідоме поле в тілі — 422, причина UNKNOWN_FIELD")
	void unknownField_returns422() throws Exception {
		// Arrange
		String requestBody = body("\"name\": \"Барсик\", \"species\": \"Кіт\", \"color\": \"рудий\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("color")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: поле id формується сервером і в запиті недопустиме — 422 UNKNOWN_FIELD")
	void serverManagedId_returns422() throws Exception {
		// Arrange
		String requestBody = body("\"id\": \"00000000-0000-4000-8000-000000000001\", \"name\": \"Барсик\", "
				+ "\"species\": \"Кіт\"");

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("id")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18: зламаний JSON — 400 MALFORMED_REQUEST")
	void malformedJson_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": ";

		// Act
		var response = postPatient(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: тіло не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void plainTextBody_returns415() throws Exception {
		// Arrange
		var request = post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("name=Барсик");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}
}
