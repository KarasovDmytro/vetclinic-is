package ua.vetclinic.api.medications;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestMedications.createMedication;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-044, AD-15, AD-17.8, AD-19.2: каталог медикаментів із пошуком, фільтром lowStock і пагінацією (listMedications). */
@ApiIntegrationTest
class ListMedicationsTest {

	@Autowired
	private MockMvc mockMvc;

	private MockHttpServletRequestBuilder listAsAdmin() {
		return get("/api/v1/medications").with(user("admin").roles("ADMIN"));
	}

	private static String tag() {
		return "Каталог" + UUID.randomUUID().toString().replace("-", "");
	}

	@Test
	@DisplayName("REQ-F-044: адміністратор отримує каталог — 200, сторінка з page, pageSize, total та items")
	void adminListsMedications_returnsPage() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, tag + " А", 1, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)))
				.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].name", equalTo(tag + " А")));
	}

	@Test
	@DisplayName("REQ-F-044: лікар теж бачить каталог, щоб обирати препарати у формі прийому — 200")
	void veterinarianListsMedications_returns200() throws Exception {
		// Arrange
		var request = get("/api/v1/medications").with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("REQ-NF-002: GET /medications без токена — 401 UNAUTHORIZED")
	void listWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/medications");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-19.2: каталог відсортований за назвою за зростанням")
	void medications_areSortedByName() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, tag + " Яблуко", 1, null);
		createMedication(mockMvc, tag + " Абрикос", 1, null);
		createMedication(mockMvc, tag + " Диня", 1, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name",
						contains(tag + " Абрикос", tag + " Диня", tag + " Яблуко")));
	}

	@Test
	@DisplayName("AD-15.2: q шукає входження підрядка без урахування регістру")
	void query_matchesSubstringIgnoringCase() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, "Амоксицилін " + tag, 1, null);
		createMedication(mockMvc, "Новокаїн " + tag, 1, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", "АМОКСИ").param("pageSize", "100"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", hasItem("Амоксицилін " + tag)))
				.andExpect(jsonPath("$.items[*].name", not(hasItem("Новокаїн " + tag))));
	}

	@Test
	@DisplayName("AD-15.2: спецсимвол % у q шукається буквально, а не як шаблон")
	void query_treatsPercentLiterally() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, tag + " 5%", 1, null);
		createMedication(mockMvc, tag + " 50", 1, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag + " 5%"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].name", equalTo(tag + " 5%")));
	}

	@Test
	@DisplayName("AD-17.8: q з 1 символу — 422, поле q, причина TOO_SHORT")
	void shortQuery_returns422() throws Exception {
		// Arrange
		var request = listAsAdmin().param("q", "а");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("q")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.8: q довше за 100 символів — 422, поле q, причина TOO_LONG")
	void longQuery_returns422() throws Exception {
		// Arrange
		var request = listAsAdmin().param("q", "а".repeat(101));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("q")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("REQ-F-043: lowStock=true повертає лише препарати із залишком нижче порога")
	void lowStockTrue_returnsOnlyLowStock() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, tag + " мало", 2, 5);
		createMedication(mockMvc, tag + " достатньо", 9, 5);
		createMedication(mockMvc, tag + " рівно", 5, 5);
		createMedication(mockMvc, tag + " без порога", 0, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag).param("lowStock", "true"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items[0].name", equalTo(tag + " мало")))
				.andExpect(jsonPath("$.items[0].lowStock", equalTo(true)));
	}

	@Test
	@DisplayName("REQ-F-043: lowStock=false повертає препарати без нестачі, зокрема без порога й із залишком, що дорівнює порогу")
	void lowStockFalse_returnsOthers() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, tag + " мало", 2, 5);
		createMedication(mockMvc, tag + " достатньо", 9, 5);
		createMedication(mockMvc, tag + " рівно", 5, 5);
		createMedication(mockMvc, tag + " без порога", 0, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag).param("lowStock", "false"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(3)))
				.andExpect(jsonPath("$.items[*].lowStock", everyItem(equalTo(false))));
	}

	@Test
	@DisplayName("AD-18.2: lowStock не boolean — 400 MALFORMED_REQUEST")
	void malformedLowStock_returns400() throws Exception {
		// Arrange
		var request = listAsAdmin().param("lowStock", "можливо");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-19.2: пагінація — друга сторінка по 2 елементи містить третій і четвертий, total дорівнює 4")
	void paging_returnsRequestedPage() throws Exception {
		// Arrange
		String tag = tag();
		for (String suffix : new String[] { "1", "2", "3", "4" }) {
			createMedication(mockMvc, tag + " " + suffix, 1, null);
		}

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag).param("page", "2").param("pageSize", "2"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(2)))
				.andExpect(jsonPath("$.pageSize", equalTo(2)))
				.andExpect(jsonPath("$.total", equalTo(4)))
				.andExpect(jsonPath("$.items[*].name", contains(tag + " 3", tag + " 4")));
	}

	@Test
	@DisplayName("AD-17.8: pageSize 101 — 422, поле pageSize, причина OUT_OF_RANGE")
	void pageSizeAboveMaximum_returns422() throws Exception {
		// Arrange
		var request = listAsAdmin().param("pageSize", "101");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("pageSize")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.8: page 0 — 422, поле page, причина OUT_OF_RANGE")
	void pageZero_returns422() throws Exception {
		// Arrange
		var request = listAsAdmin().param("page", "0");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("page")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-19.2: сторінка за межами результатів — 200, порожній items, total зберігається")
	void pageBeyondResults_returnsEmptyItems() throws Exception {
		// Arrange
		String tag = tag();
		createMedication(mockMvc, tag, 1, null);

		// Act
		var response = mockMvc.perform(listAsAdmin().param("q", tag).param("page", "5"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.total", equalTo(1)));
	}
}
