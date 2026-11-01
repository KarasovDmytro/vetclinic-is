package ua.vetclinic.api.medications;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestData.CLINIC_OFFSET_TIMESTAMP;
import static ua.vetclinic.api.TestData.UUID_PATTERN;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-040, REQ-F-043, AD-13, AD-17.7: додавання медикамента до каталогу та перегляд (createMedication, getMedication). */
@ApiIntegrationTest
class CreateMedicationTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postMedication(String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/medications")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("REQ-F-040: адміністратор додає медикамент — 201, id, назва, залишок і поріг, Location на ресурс")
	void adminCreatesMedication_returns201() throws Exception {
		// Arrange
		String name = uniqueName("Амоксицилін");
		String requestBody = "{ \"name\": \"" + name + "\", \"quantity\": 10, \"minThreshold\": 3 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(header().string("Location", matchesPattern("/api/v1/medications/" + UUID_PATTERN)))
				.andExpect(jsonPath("$.id", matchesPattern(UUID_PATTERN)))
				.andExpect(jsonPath("$.name", equalTo(name)))
				.andExpect(jsonPath("$.quantity", equalTo(10)))
				.andExpect(jsonPath("$.minThreshold", equalTo(3)));
	}

	@Test
	@DisplayName("REQ-F-043: поріг необов'язковий — без нього minThreshold дорівнює null, а lowStock — false")
	void createWithoutThreshold_returnsNullThreshold() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Без порога") + "\", \"quantity\": 0 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.minThreshold", nullValue()))
				.andExpect(jsonPath("$.lowStock", equalTo(false)));
	}

	@Test
	@DisplayName("REQ-F-043: поріг явно null — приймається як «поріг не заданий»")
	void createWithNullThreshold_returnsNullThreshold() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Null поріг") + "\", \"quantity\": 5, \"minThreshold\": null }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.minThreshold", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-043: залишок нижчий за поріг одразу після створення — lowStock дорівнює true")
	void quantityBelowThreshold_marksLowStock() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Мало") + "\", \"quantity\": 2, \"minThreshold\": 5 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lowStock", equalTo(true)));
	}

	@Test
	@DisplayName("AD-02.2: залишок дорівнює порогу — lowStock дорівнює false (поріг строгий)")
	void quantityEqualToThreshold_isNotLowStock() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Рівно") + "\", \"quantity\": 5, \"minThreshold\": 5 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.lowStock", equalTo(false)));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях назви обрізаються")
	void nameIsTrimmed() throws Exception {
		// Arrange
		String name = uniqueName("Обрізання");
		String requestBody = "{ \"name\": \"   " + name + "  \", \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name", equalTo(name)));
	}

	@Test
	@DisplayName("AD-19.1: createdAt і updatedAt — ISO 8601 із зсувом часового поясу клініки, при створенні рівні")
	void timestamps_areClinicOffsetAndEqual() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Час") + "\", \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)));
	}

	@Test
	@DisplayName("AD-13: назва вже є в каталозі — 409 MEDICATION_NAME_TAKEN")
	void duplicateName_returns409() throws Exception {
		// Arrange
		String name = uniqueName("Дубль");
		createMedication(mockMvc, name, 1, null);

		// Act
		var response = postMedication("{ \"name\": \"" + name + "\", \"quantity\": 2 }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("MEDICATION_NAME_TAKEN")));
	}

	@Test
	@DisplayName("AD-13: унікальність без урахування регістру — «АСПІРИН» після «аспірин» дає 409")
	void duplicateNameWithOtherCase_returns409() throws Exception {
		// Arrange
		String name = uniqueName("аспірин");
		createMedication(mockMvc, name, 1, null);

		// Act
		var response = postMedication("{ \"name\": \"" + name.toUpperCase() + "\", \"quantity\": 2 }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("MEDICATION_NAME_TAKEN")));
	}

	@Test
	@DisplayName("AD-13, AD-17.1: назва з пробілами по краях збігається з існуючою — 409")
	void duplicateNameWithSpaces_returns409() throws Exception {
		// Arrange
		String name = uniqueName("Пробіли");
		createMedication(mockMvc, name, 1, null);

		// Act
		var response = postMedication("{ \"name\": \"  " + name + "  \", \"quantity\": 2 }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("MEDICATION_NAME_TAKEN")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може додавати медикаменти — 403 FORBIDDEN")
	void veterinarianCreatesMedication_returns403() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Лікар") + "\", \"quantity\": 1 }";

		// Act
		var response = mockMvc.perform(post("/api/v1/medications")
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-NF-002: POST /medications без токена — 401 UNAUTHORIZED")
	void createWithoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/medications")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"Анонім\", \"quantity\": 1 }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-18.2: тіло не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void textBody_returns415() throws Exception {
		// Arrange
		var request = post("/api/v1/medications")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("name=Текст");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}

	@Test
	@DisplayName("REQ-F-040: адміністратор отримує медикамент за id — 200 з усіма полями")
	void adminGetsMedication_returns200() throws Exception {
		// Arrange
		String name = uniqueName("Перегляд");
		String medicationId = createMedication(mockMvc, name, 7, 2);

		// Act
		var response = mockMvc.perform(get("/api/v1/medications/" + medicationId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(medicationId)))
				.andExpect(jsonPath("$.name", equalTo(name)))
				.andExpect(jsonPath("$.quantity", equalTo(7)))
				.andExpect(jsonPath("$.minThreshold", equalTo(2)))
				.andExpect(jsonPath("$.lowStock", equalTo(false)));
	}

	@Test
	@DisplayName("REQ-F-044: лікар теж бачить медикамент за id — 200")
	void veterinarianGetsMedication_returns200() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Лікар читає"), 1, null);

		// Act
		var response = mockMvc.perform(get("/api/v1/medications/" + medicationId)
				.with(user("vet").roles("VETERINARIAN")));

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-18.2: медикамента з таким id немає — 404 NOT_FOUND")
	void unknownMedication_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000a01";

		// Act
		var response = mockMvc.perform(get("/api/v1/medications/" + unknownId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.2: id не UUID — 400 MALFORMED_REQUEST")
	void malformedMedicationId_returns400() throws Exception {
		// Arrange
		String malformedId = "не-uuid";

		// Act
		var response = mockMvc.perform(get("/api/v1/medications/" + malformedId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /medications/{id} без токена — 401 UNAUTHORIZED")
	void getWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/medications/00000000-0000-4000-8000-000000000a02");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-09: DELETE /medications/{id} — 405 METHOD_NOT_ALLOWED, фізичного видалення немає")
	void deleteMedication_returns405() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Видалення"), 1, null);

		// Act
		var response = mockMvc.perform(delete("/api/v1/medications/" + medicationId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("GET")));
	}
}
