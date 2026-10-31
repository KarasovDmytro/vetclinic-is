package ua.vetclinic.api.medications;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.quantityOf;
import static ua.vetclinic.api.TestMedications.uniqueName;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-041, REQ-F-043, AD-08.3, AD-13, AD-17.7, AD-19.3: редагування медикамента (операція updateMedication). */
@ApiIntegrationTest
class UpdateMedicationTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions patchMedication(String medicationId, String requestBody) throws Exception {
		return mockMvc.perform(patch("/api/v1/medications/" + medicationId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("REQ-F-041: зміна назви — 200, нова назва, поріг і залишок без змін")
	void renameMedication_changesOnlyName() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Стара назва"), 10, 3);
		String newName = uniqueName("Нова назва");

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"" + newName + "\" }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", equalTo(newName)))
				.andExpect(jsonPath("$.quantity", equalTo(10)))
				.andExpect(jsonPath("$.minThreshold", equalTo(3)));
	}

	@Test
	@DisplayName("REQ-F-043: зміна порога — 200, новий поріг, назва без змін")
	void changeThreshold_changesOnlyThreshold() throws Exception {
		// Arrange
		String name = uniqueName("Зміна порога");
		String medicationId = createMedication(mockMvc, name, 10, 3);

		// Act
		var response = patchMedication(medicationId, "{ \"minThreshold\": 8 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", equalTo(name)))
				.andExpect(jsonPath("$.minThreshold", equalTo(8)));
	}

	@Test
	@DisplayName("AD-19.3: minThreshold null очищає поріг")
	void nullThreshold_clearsThreshold() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Очищення порога"), 1, 5);

		// Act
		var response = patchMedication(medicationId, "{ \"minThreshold\": null }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.minThreshold", nullValue()))
				.andExpect(jsonPath("$.lowStock", equalTo(false)));
	}

	@Test
	@DisplayName("REQ-F-043: підвищення порога вище за залишок — lowStock стає true")
	void raiseThresholdAboveQuantity_setsLowStock() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Підвищення порога"), 4, 2);

		// Act
		var response = patchMedication(medicationId, "{ \"minThreshold\": 10 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.lowStock", equalTo(true)));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях нової назви обрізаються")
	void newNameIsTrimmed() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Обрізання назви"), 1, null);
		String newName = uniqueName("Обрізана");

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"  " + newName + "  \" }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", equalTo(newName)));
	}

	@Test
	@DisplayName("AD-19.3: updatedAt після редагування оновлюється, createdAt лишається")
	void update_refreshesUpdatedAtOnly() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Час редагування"), 1, null);
		String before = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.get("/api/v1/medications/" + medicationId).with(user("admin").roles("ADMIN")))
				.andReturn().getResponse().getContentAsString();

		// Act
		String after = patchMedication(medicationId, "{ \"minThreshold\": 1 }")
				.andReturn().getResponse().getContentAsString();

		// Assert
		org.hamcrest.MatcherAssert.assertThat((String) JsonPath.read(after, "$.createdAt"),
				equalTo(JsonPath.read(before, "$.createdAt")));
		org.hamcrest.MatcherAssert.assertThat((String) JsonPath.read(after, "$.updatedAt"),
				not(equalTo((String) JsonPath.read(before, "$.updatedAt"))));
	}

	@Test
	@DisplayName("AD-13: нова назва вже є в каталозі — 409 MEDICATION_NAME_TAKEN")
	void renameToExistingName_returns409() throws Exception {
		// Arrange
		String takenName = uniqueName("Зайнята назва");
		createMedication(mockMvc, takenName, 1, null);
		String medicationId = createMedication(mockMvc, uniqueName("Перейменування"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"" + takenName + "\" }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("MEDICATION_NAME_TAKEN")));
	}

	@Test
	@DisplayName("AD-13: нова назва збігається з чужою без урахування регістру — 409 MEDICATION_NAME_TAKEN")
	void renameToExistingNameInOtherCase_returns409() throws Exception {
		// Arrange
		String takenName = uniqueName("регістр чужий");
		createMedication(mockMvc, takenName, 1, null);
		String medicationId = createMedication(mockMvc, uniqueName("Перейменування регістру"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"" + takenName.toUpperCase() + "\" }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("MEDICATION_NAME_TAKEN")));
	}

	@Test
	@DisplayName("AD-13: перейменування на власну назву (або зміна її регістру) не вважається конфліктом")
	void renameToOwnNameInOtherCase_returns200() throws Exception {
		// Arrange
		String name = uniqueName("власна назва");
		String medicationId = createMedication(mockMvc, name, 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"" + name.toUpperCase() + "\" }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.name", equalTo(name.toUpperCase())));
	}

	@Test
	@DisplayName("AD-08.3: залишок не змінюється редагуванням — quantity у запиті дає 422 IMMUTABLE_FIELD")
	void quantityInRequest_returns422ImmutableField() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Незмінний залишок"), 10, null);

		// Act
		var response = patchMedication(medicationId, "{ \"quantity\": 99 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("IMMUTABLE_FIELD")));
		org.hamcrest.MatcherAssert.assertThat(quantityOf(mockMvc, medicationId), equalTo(10));
	}

	@Test
	@DisplayName("AD-19.3: порожнє тіло {} — 422, причина EMPTY_BODY")
	void emptyBody_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Порожнє тіло"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{}");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("EMPTY_BODY")));
	}

	@Test
	@DisplayName("AD-19.3: невідоме поле — 422, причина UNKNOWN_FIELD")
	void unknownField_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Невідоме поле"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"unit\": \"мг\" }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("unit")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: назва явно null — 422, поле name, причина REQUIRED")
	void nullName_returns422Required() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Null назва"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"name\": null }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: назва з самих пробілів стає порожньою — 422, поле name, причина TOO_SHORT")
	void blankName_returns422TooShort() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Порожня назва"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"   \" }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.7: назва довша за 150 символів — 422, поле name, причина TOO_LONG")
	void tooLongName_returns422TooLong() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Довга назва"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"name\": \"" + "В".repeat(151) + "\" }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.7: поріг понад 1 000 000 — 422, поле minThreshold, причина OUT_OF_RANGE")
	void thresholdAboveMaximum_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Завеликий поріг"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"minThreshold\": 1000001 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("minThreshold")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.7: від'ємний поріг — 422, поле minThreshold, причина OUT_OF_RANGE")
	void negativeThreshold_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Від'ємний поріг"), 1, null);

		// Act
		var response = patchMedication(medicationId, "{ \"minThreshold\": -1 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("minThreshold")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-18.4: медикамента немає — 404 NOT_FOUND")
	void unknownMedication_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000a04";

		// Act
		var response = patchMedication(unknownId, "{ \"minThreshold\": 1 }");

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — некоректне тіло для неіснуючого медикамента дає 422, а не 404")
	void invalidBodyForUnknownMedication_returns422BeforeNotFound() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000a05";

		// Act
		var response = patchMedication(unknownId, "{ \"minThreshold\": -1 }");

		// Assert
		response.andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може редагувати медикаменти — 403 FORBIDDEN")
	void veterinarianPatchesMedication_returns403() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Лікар редагує"), 1, null);

		// Act
		var response = mockMvc.perform(patch("/api/v1/medications/" + medicationId)
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"minThreshold\": 1 }"));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-NF-002: PATCH /medications/{id} без токена — 401 UNAUTHORIZED")
	void patchWithoutToken_returns401() throws Exception {
		// Arrange
		var request = patch("/api/v1/medications/00000000-0000-4000-8000-000000000a06")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"minThreshold\": 1 }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}
}
