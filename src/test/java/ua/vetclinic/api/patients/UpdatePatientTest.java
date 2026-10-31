package ua.vetclinic.api.patients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-013, REQ-F-014, AD-19.3: часткове редагування базових даних пацієнта (операція updatePatient). */
@ApiIntegrationTest
class UpdatePatientTest {

	@Autowired
	private MockMvc mockMvc;

	private String ownerId;

	private String patientId;

	@BeforeEach
	void createPatientToEdit() throws Exception {
		ownerId = createOwner(mockMvc, "Власник Редагування", "+380501234600");
		var created = mockMvc.perform(post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "ownerId": "%s",
						  "name": "Початкове Імʼя",
						  "species": "Кіт",
						  "breed": "Сіамська",
						  "sex": "MALE",
						  "birthDate": "2020-01-10"
						}
						""".formatted(ownerId)))
				.andExpect(status().isCreated())
				.andReturn();
		patientId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	private ResultActions patchPatient(String requestBody) throws Exception {
		return mockMvc.perform(patch("/api/v1/patients/" + patientId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	private ResultActions getPatient() throws Exception {
		return mockMvc.perform(get("/api/v1/patients/" + patientId).with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("REQ-F-013: адміністратор змінює ім'я — 200, ім'я оновлено, власник і статус без змін")
	void updateName_returns200AndKeepsOwnerAndStatus() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"Нове Імʼя\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(patientId)))
				.andExpect(jsonPath("$.name", equalTo("Нове Імʼя")))
				.andExpect(jsonPath("$.ownerId", equalTo(ownerId)))
				.andExpect(jsonPath("$.status", equalTo("ACTIVE")))
				.andExpect(jsonPath("$.species", equalTo("Кіт")));
	}

	@Test
	@DisplayName("REQ-F-013: адміністратор змінює всі базові поля одразу — 200, нові значення збережено")
	void updateAllFields_returns200AndPersists() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "name": "Інше Імʼя",
				  "species": "Собака",
				  "breed": "Лабрадор",
				  "sex": "FEMALE",
				  "birthDate": "2019-06-30"
				}
				""";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.species", equalTo("Собака")))
				.andExpect(jsonPath("$.breed", equalTo("Лабрадор")))
				.andExpect(jsonPath("$.sex", equalTo("FEMALE")))
				.andExpect(jsonPath("$.birthDate", equalTo("2019-06-30")));
		getPatient().andExpect(jsonPath("$.name", equalTo("Інше Імʼя")))
				.andExpect(jsonPath("$.breed", equalTo("Лабрадор")));
	}

	@Test
	@DisplayName("AD-19.3: відсутні в запиті поля лишаються без змін")
	void absentFields_remainUnchanged() throws Exception {
		// Arrange
		String requestBody = "{ \"species\": \"Тхір\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", equalTo("Початкове Імʼя")))
				.andExpect(jsonPath("$.breed", equalTo("Сіамська")))
				.andExpect(jsonPath("$.sex", equalTo("MALE")))
				.andExpect(jsonPath("$.birthDate", equalTo("2020-01-10")));
	}

	@Test
	@DisplayName("AD-19.3: breed: null очищає породу")
	void nullBreed_clearsBreed() throws Exception {
		// Arrange
		String requestBody = "{ \"breed\": null }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.breed", nullValue()));
		getPatient().andExpect(jsonPath("$.breed", nullValue()));
	}

	@Test
	@DisplayName("AD-19.3: birthDate: null очищає дату народження")
	void nullBirthDate_clearsBirthDate() throws Exception {
		// Arrange
		String requestBody = "{ \"birthDate\": null }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.birthDate", nullValue()));
		getPatient().andExpect(jsonPath("$.birthDate", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-013: createdAt не змінюється, updatedAt після редагування збільшується")
	void update_keepsCreatedAtAndMovesUpdatedAt() throws Exception {
		// Arrange
		String before = getPatient().andReturn().getResponse().getContentAsString();

		// Act
		String after = patchPatient("{ \"name\": \"Часові Мітки\" }")
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// Assert
		assertThat((String) JsonPath.read(after, "$.createdAt"), equalTo(JsonPath.read(before, "$.createdAt")));
		long updatedBefore = OffsetDateTime.parse((String) JsonPath.read(before, "$.updatedAt")).toInstant()
				.toEpochMilli();
		long updatedAfter = OffsetDateTime.parse((String) JsonPath.read(after, "$.updatedAt")).toInstant()
				.toEpochMilli();
		assertThat(updatedAfter, greaterThan(updatedBefore));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях значень при редагуванні обрізаються")
	void surroundingWhitespace_isTrimmed() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"  Обрізане Імʼя  \", \"species\": \" Кіт \", \"breed\": \" Мейн-кун \" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", equalTo("Обрізане Імʼя")))
				.andExpect(jsonPath("$.species", equalTo("Кіт")))
				.andExpect(jsonPath("$.breed", equalTo("Мейн-кун")));
	}

	@Test
	@DisplayName("REQ-F-014: зміна власника заборонена — ownerId у тілі дає 422, поле ownerId, причина IMMUTABLE_FIELD")
	void ownerIdInBody_returns422ImmutableField() throws Exception {
		// Arrange
		String otherOwnerId = createOwner(mockMvc, "Інший Власник", "+380501234601");
		String requestBody = "{ \"ownerId\": \"" + otherOwnerId + "\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("ownerId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("IMMUTABLE_FIELD")));
		getPatient().andExpect(jsonPath("$.ownerId", equalTo(ownerId)));
	}

	@Test
	@DisplayName("REQ-F-014: навіть той самий ownerId у тілі відхиляється — IMMUTABLE_FIELD")
	void sameOwnerIdInBody_returns422ImmutableField() throws Exception {
		// Arrange
		String requestBody = "{ \"ownerId\": \"" + ownerId + "\", \"name\": \"Хочу Змінити\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("ownerId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("IMMUTABLE_FIELD")));
		getPatient().andExpect(jsonPath("$.name", equalTo("Початкове Імʼя")));
	}

	@Test
	@DisplayName("AD-19.4: статус не можна змінити полем PATCH — status у тілі дає 422 IMMUTABLE_FIELD")
	void statusInBody_returns422ImmutableField() throws Exception {
		// Arrange
		String requestBody = "{ \"status\": \"ARCHIVED\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("status")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("IMMUTABLE_FIELD")));
		getPatient().andExpect(jsonPath("$.status", equalTo("ACTIVE")));
	}

	@Test
	@DisplayName("AD-18: кілька порушень разом — ownerId, status і name: null повертаються в одному details")
	void severalViolations_areAllReported() throws Exception {
		// Arrange
		String requestBody = "{ \"ownerId\": \"" + ownerId + "\", \"status\": \"ARCHIVED\", \"name\": null }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(3)));
	}

	@Test
	@DisplayName("AD-19.3: порожнє тіло {} — 422 з причиною EMPTY_BODY")
	void emptyBody_returns422EmptyBody() throws Exception {
		// Arrange
		String requestBody = "{}";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("EMPTY_BODY")));
	}

	@Test
	@DisplayName("AD-17.1: name: null — обов'язкове поле не буває null, 422 REQUIRED")
	void nullName_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": null }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: species: null — 422 REQUIRED")
	void nullSpecies_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"species\": null }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("species")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: sex: null — 422 REQUIRED")
	void nullSex_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"sex\": null }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("sex")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.4: порожнє ім'я — 422, поле name, причина TOO_SHORT")
	void emptyName_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.4: ім'я довше за 100 символів — 422, поле name, причина TOO_LONG")
	void tooLongName_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + "Б".repeat(101) + "\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.4: порожній вид — 422, поле species, причина TOO_SHORT")
	void emptySpecies_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"species\": \"\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("species")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.4: вид довший за 50 символів — 422, поле species, причина TOO_LONG")
	void tooLongSpecies_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"species\": \"" + "В".repeat(51) + "\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("species")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.1: порожня порода замість null — 422, поле breed, причина TOO_SHORT")
	void emptyBreed_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"breed\": \"\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("breed")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.4: порода довша за 100 символів — 422, поле breed, причина TOO_LONG")
	void tooLongBreed_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"breed\": \"" + "П".repeat(101) + "\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("breed")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-18: невідоме значення sex — 422, поле sex, причина INVALID_ENUM")
	void invalidSex_returns422InvalidEnum() throws Exception {
		// Arrange
		String requestBody = "{ \"sex\": \"HERMAPHRODITE\" }";

		// Act
		var response = patchPatient(requestBody);

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
		String requestBody = "{ \"birthDate\": \"" + tomorrow + "\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("birthDate")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-18.2: дата народження не у форматі YYYY-MM-DD — 400 MALFORMED_REQUEST")
	void birthDateWithWrongFormat_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"birthDate\": \"10/01/2020\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-19.3: невідоме поле в тілі — 422 UNKNOWN_FIELD")
	void unknownField_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = "{ \"color\": \"рудий\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("color")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: поле id у тілі — 422 UNKNOWN_FIELD")
	void idInBody_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = "{ \"id\": \"00000000-0000-4000-8000-000000000001\" }";

		// Act
		var response = patchPatient(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("id")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("REQ-F-013: невдале редагування нічого не змінює — дані пацієнта лишаються попередніми")
	void rejectedUpdate_leavesPatientUnchanged() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"Не Має Зберегтись\", \"species\": \"" + "В".repeat(51) + "\" }";

		// Act
		patchPatient(requestBody).andExpect(status().isUnprocessableContent());

		// Assert
		getPatient().andExpect(jsonPath("$.name", equalTo("Початкове Імʼя")))
				.andExpect(jsonPath("$.species", not(equalTo("В".repeat(51)))));
	}

	@Test
	@DisplayName("AD-18: PATCH без тіла — 400 MALFORMED_REQUEST")
	void missingBody_returns400() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/" + patientId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON);

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: PATCH із тілом не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void plainTextBody_returns415() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/" + patientId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("name=Інше");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}

	@Test
	@DisplayName("REQ-F-013: неіснуючий пацієнт — 404 NOT_FOUND")
	void unknownPatient_returns404() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/00000000-0000-4000-8000-000000000511")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Невідомий\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор пацієнта не UUID — 400 MALFORMED_REQUEST")
	void patientIdNotUuid_returns400() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/not-a-uuid")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Невірний Id\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може редагувати пацієнтів — 403 FORBIDDEN")
	void veterinarianUpdatesPatient_returns403() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/" + patientId)
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Лікарська Правка\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		getPatient().andExpect(jsonPath("$.name", equalTo("Початкове Імʼя")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — лікар на неіснуючому пацієнті отримує 403, а не 404")
	void veterinarianUpdatesUnknownPatient_returns403Before404() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/00000000-0000-4000-8000-000000000511")
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Перевірка Порядку\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — невалідне тіло для неіснуючого пацієнта дає 422, а не 404")
	void invalidBodyForUnknownPatient_returns422Before404() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/00000000-0000-4000-8000-000000000511")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"status\": \"ARCHIVED\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("IMMUTABLE_FIELD")));
	}

	@Test
	@DisplayName("REQ-NF-002: PATCH /patients/{id} без токена — 401 UNAUTHORIZED")
	void updateWithoutToken_returns401() throws Exception {
		// Arrange
		var request = patch("/api/v1/patients/" + patientId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Без Токена\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}
}
