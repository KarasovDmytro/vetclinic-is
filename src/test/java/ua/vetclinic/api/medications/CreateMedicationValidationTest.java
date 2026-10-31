package ua.vetclinic.api.medications;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestMedications.uniqueName;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-17.1, AD-17.7, AD-18: правила полів медикамента (операція createMedication). */
@ApiIntegrationTest
class CreateMedicationValidationTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions postMedication(String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/medications")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	private String body(String fields) {
		return "{ \"name\": \"" + uniqueName("Валідація") + "\", " + fields + " }";
	}

	@Test
	@DisplayName("AD-17.7: назву не вказано — 422, поле name, причина REQUIRED")
	void missingName_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: назва явно null — 422, поле name, причина REQUIRED")
	void nullName_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": null, \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: назва з самих пробілів — 422, поле name, причина REQUIRED")
	void blankName_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"   \", \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.7: назва довша за 150 символів — 422, поле name, причина TOO_LONG")
	void tooLongName_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + "А".repeat(151) + "\", \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.7: назва рівно з 150 символів приймається — 201")
	void nameOfMaxLength_returns201() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + "Б".repeat(150) + "\", \"quantity\": 1 }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-17.7: початковий залишок не вказано — 422, поле quantity, причина REQUIRED")
	void missingQuantity_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": \"" + uniqueName("Без залишку") + "\" }";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: залишок явно null — 422, поле quantity, причина REQUIRED")
	void nullQuantity_returns422Required() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": null");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("REQ-F-048, AD-17.7: від'ємний початковий залишок — 422, поле quantity, причина OUT_OF_RANGE")
	void negativeQuantity_returns422OutOfRange() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": -1");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.7: залишок понад 1 000 000 — 422, поле quantity, причина OUT_OF_RANGE")
	void quantityAboveMaximum_returns422OutOfRange() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1000001");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.7: залишок рівно 1 000 000 приймається — 201")
	void quantityOfMaximum_returns201() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1000000");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-17.7: від'ємний поріг — 422, поле minThreshold, причина OUT_OF_RANGE")
	void negativeThreshold_returns422OutOfRange() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1, \"minThreshold\": -1");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("minThreshold")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.7: поріг понад 1 000 000 — 422, поле minThreshold, причина OUT_OF_RANGE")
	void thresholdAboveMaximum_returns422OutOfRange() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1, \"minThreshold\": 1000001");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("minThreshold")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-18.2: залишок не число — 400 MALFORMED_REQUEST")
	void nonNumericQuantity_returns400() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": \"багато\"");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-08.1: дробовий залишок — 400 MALFORMED_REQUEST, залишок є цілим числом")
	void fractionalQuantity_returns400() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1.5");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-17.1: lowStock формується сервером — у запиті 422 UNKNOWN_FIELD")
	void lowStockInRequest_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1, \"lowStock\": true");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("lowStock")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: id формується сервером — у запиті 422 UNKNOWN_FIELD")
	void idInRequest_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1, \"id\": \"00000000-0000-4000-8000-000000000a03\"");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("id")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-19.3: невідоме поле — 422, поле unit, причина UNKNOWN_FIELD (одиниці виміру не моделюються)")
	void unknownField_returns422() throws Exception {
		// Arrange
		String requestBody = body("\"quantity\": 1, \"unit\": \"мг\"");

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("unit")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18.2: зламаний JSON — 400 MALFORMED_REQUEST")
	void brokenJson_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"name\": ";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.6: порожній об'єкт — 422, обидва обов'язкові поля name і quantity в details")
	void emptyObject_listsBothRequiredFields() throws Exception {
		// Arrange
		String requestBody = "{}";

		// Act
		var response = postMedication(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(2)))
				.andExpect(jsonPath("$.details[*].reason", org.hamcrest.Matchers.everyItem(equalTo("REQUIRED"))));
	}
}
