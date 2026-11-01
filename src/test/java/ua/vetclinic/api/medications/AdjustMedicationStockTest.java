package ua.vetclinic.api.medications;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.quantityOf;
import static ua.vetclinic.api.TestMedications.uniqueName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-042, REQ-F-048, AD-08.2, AD-17.7: ручне коригування залишку зміною (операція adjustMedicationStock). */
@ApiIntegrationTest
class AdjustMedicationStockTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions adjust(String medicationId, String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/medications/" + medicationId + "/adjust-stock")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("REQ-F-042: додатна зміна збільшує залишок — 200, залишок 7 + 5 = 12")
	void positiveDelta_increasesQuantity() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Надходження"), 7, 3);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 5 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(medicationId)))
				.andExpect(jsonPath("$.quantity", equalTo(12)))
				.andExpect(jsonPath("$.minThreshold", equalTo(3)))
				.andExpect(jsonPath("$.lowStock", equalTo(false)));
	}

	@Test
	@DisplayName("REQ-F-042: від'ємна зміна зменшує залишок — 200, залишок 10 − 4 = 6")
	void negativeDelta_decreasesQuantity() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Списання вручну"), 10, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": -4 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity", equalTo(6)));
	}

	@Test
	@DisplayName("REQ-F-048: зменшення рівно до нуля допустиме — 200, залишок 0")
	void deltaToExactlyZero_returns200() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("До нуля"), 3, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": -3 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity", equalTo(0)));
	}

	@Test
	@DisplayName("REQ-F-048: результат менший за 0 — 409 NEGATIVE_STOCK, залишок не змінюється")
	void deltaBelowZero_returns409AndKeepsQuantity() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Мінус"), 3, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": -4 }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("NEGATIVE_STOCK")));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(3));
	}

	@Test
	@DisplayName("REQ-F-043: коригування униз під поріг — lowStock стає true")
	void decreaseBelowThreshold_setsLowStock() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Під поріг"), 10, 5);

		// Act
		var response = adjust(medicationId, "{ \"delta\": -6 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity", equalTo(4)))
				.andExpect(jsonPath("$.lowStock", equalTo(true)));
	}

	@Test
	@DisplayName("REQ-F-043: коригування вгору над порогом — lowStock знову false")
	void increaseAboveThreshold_clearsLowStock() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Над поріг"), 1, 5);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 10 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity", equalTo(11)))
				.andExpect(jsonPath("$.lowStock", equalTo(false)));
	}

	@Test
	@DisplayName("AD-17.7: нульова зміна — 422, поле delta, причина ZERO_DELTA")
	void zeroDelta_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Нуль"), 1, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 0 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("delta")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("ZERO_DELTA")));
	}

	@Test
	@DisplayName("AD-17.7: зміну не вказано — 422, поле delta, причина REQUIRED")
	void missingDelta_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Без зміни"), 1, null);

		// Act
		var response = adjust(medicationId, "{}");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("delta")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: delta явно null — 422, поле delta, причина REQUIRED")
	void nullDelta_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Null зміна"), 1, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": null }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("delta")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.7: |delta| понад 1 000 000 — 422, поле delta, причина OUT_OF_RANGE")
	void deltaAboveMaximum_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Завелика зміна"), 1, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 1000001 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("delta")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.7: delta менша за −1 000 000 — 422, поле delta, причина OUT_OF_RANGE")
	void deltaBelowMinimum_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Занизька зміна"), 1, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": -1000001 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("delta")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.7: результат понад 1 000 000 — 422, поле delta, причина OUT_OF_RANGE, залишок не змінюється")
	void resultAboveMaximum_returns422AndKeepsQuantity() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Переповнення"), 999999, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 2 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("delta")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(999999));
	}

	@Test
	@DisplayName("AD-17.7: результат рівно 1 000 000 допустимий — 200")
	void resultOfMaximum_returns200() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Межа"), 999999, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 1 }");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.quantity", equalTo(1000000)));
	}

	@Test
	@DisplayName("AD-08.1: дробова зміна — 400 MALFORMED_REQUEST, залишок є цілим числом")
	void fractionalDelta_returns400() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Дробова зміна"), 5, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 1.5 }");

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-08.2: нове абсолютне значення замість зміни не приймається — quantity у запиті 422 UNKNOWN_FIELD")
	void absoluteQuantity_returns422UnknownField() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Абсолютне значення"), 5, null);

		// Act
		var response = adjust(medicationId, "{ \"delta\": 1, \"quantity\": 100 }");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18.4: медикамента немає — 404 NOT_FOUND")
	void unknownMedication_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000a07";

		// Act
		var response = adjust(unknownId, "{ \"delta\": 1 }");

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
		var response = adjust(malformedId, "{ \"delta\": 1 }");

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може коригувати залишок — 403 FORBIDDEN")
	void veterinarianAdjusts_returns403() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Лікар коригує"), 5, null);

		// Act
		var response = mockMvc.perform(post("/api/v1/medications/" + medicationId + "/adjust-stock")
				.with(user("vet").roles("VETERINARIAN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"delta\": 1 }"));

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(5));
	}

	@Test
	@DisplayName("REQ-NF-002: adjust-stock без токена — 401 UNAUTHORIZED")
	void adjustWithoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/medications/00000000-0000-4000-8000-000000000a08/adjust-stock")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"delta\": 1 }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-08.2: паралельні коригування не втрачаються — 10 потоків по +1 дають залишок 10")
	void concurrentAdjustments_areNotLost() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Паралельні"), 0, null);
		ExecutorService executor = Executors.newFixedThreadPool(10);
		List<Callable<Integer>> adjustments = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			adjustments.add(() -> adjust(medicationId, "{ \"delta\": 1 }").andReturn().getResponse().getStatus());
		}

		// Act
		List<Future<Integer>> results = executor.invokeAll(adjustments);
		executor.shutdown();

		// Assert
		for (Future<Integer> result : results) {
			assertThat(result.get(), equalTo(200));
		}
		assertThat(quantityOf(mockMvc, medicationId), equalTo(10));
	}
}
