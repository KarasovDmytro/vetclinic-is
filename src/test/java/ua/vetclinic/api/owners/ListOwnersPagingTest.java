package ua.vetclinic.api.owners;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-15, AD-17.8, AD-19.2: пагінація, сортування та правила параметрів списку власників. */
@ApiIntegrationTest
class ListOwnersPagingTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions listOwners(String... paramsNameValue) throws Exception {
		var request = get("/api/v1/owners").with(user("admin").roles("ADMIN"));
		for (int i = 0; i < paramsNameValue.length; i += 2) {
			request.param(paramsNameValue[i], paramsNameValue[i + 1]);
		}
		return mockMvc.perform(request);
	}

	@Test
	@DisplayName("AD-17.8: pageSize менший за 1 — 422, поле pageSize, причина OUT_OF_RANGE")
	void zeroPageSize_returns422OutOfRange() throws Exception {
		// Arrange
		String pageSize = "0";

		// Act
		var response = listOwners("pageSize", pageSize);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("pageSize")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.8: pageSize більший за 100 — 422, поле pageSize, причина OUT_OF_RANGE")
	void tooLargePageSize_returns422OutOfRange() throws Exception {
		// Arrange
		String pageSize = "101";

		// Act
		var response = listOwners("pageSize", pageSize);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("pageSize")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.8: page менший за 1 — 422, поле page, причина OUT_OF_RANGE")
	void zeroPage_returns422OutOfRange() throws Exception {
		// Arrange
		String page = "0";

		// Act
		var response = listOwners("page", page);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("page")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-18.2: page не число — 400 MALFORMED_REQUEST")
	void nonNumericPage_returns400() throws Exception {
		// Arrange
		String page = "abc";

		// Act
		var response = listOwners("page", page);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-17.8: pageSize=100 — верхня межа допустима, 200")
	void maximumPageSize_returns200() throws Exception {
		// Arrange
		String pageSize = "100";

		// Act
		var response = listOwners("pageSize", pageSize);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.pageSize", equalTo(100)));
	}

	@Test
	@DisplayName("AD-17.8: q довший за 100 символів — 422, поле q, причина TOO_LONG")
	void tooLongQuery_returns422TooLong() throws Exception {
		// Arrange
		String query = "а".repeat(101);

		// Act
		var response = listOwners("q", query);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("q")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-15.1: q із двох символів — нижня межа допустима, 200")
	void twoCharacterQuery_returns200() throws Exception {
		// Arrange
		String query = "ко";

		// Act
		var response = listOwners("q", query);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-15: пошук без збігів — 200, total 0 і порожній items")
	void queryWithoutMatches_returnsEmptyPage() throws Exception {
		// Arrange
		String query = "Неіснуючеприз'вище-ЯЯЯ";

		// Act
		var response = listOwners("q", query);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)))
				.andExpect(jsonPath("$.items", empty()));
	}

	@Test
	@DisplayName("AD-15.2: символи % та _ у q шукаються буквально, а не як шаблон")
	void wildcardCharactersInQuery_areLiteral() throws Exception {
		// Arrange
		createOwner(mockMvc, "Шаблонний Власник", "+380501234567");
		String query = "%%";

		// Act
		var response = listOwners("q", query);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)));
	}

	@Test
	@DisplayName("AD-19.2: власники відсортовані за ПІБ за зростанням")
	void owners_areSortedByFullNameAscending() throws Exception {
		// Arrange
		createOwner(mockMvc, "Сортувальник Яблуко", "+380501234567");
		createOwner(mockMvc, "Сортувальник Абрикос", "+380501234568");
		createOwner(mockMvc, "Сортувальник Мандарин", "+380501234569");

		// Act
		var response = listOwners("q", "Сортувальник");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].fullName", contains("Сортувальник Абрикос", "Сортувальник Мандарин",
						"Сортувальник Яблуко")));
	}

	@Test
	@DisplayName("AD-15.4: page і pageSize вибирають зріз відсортованого результату, total — загальна кількість")
	void paging_returnsRequestedSliceAndTotal() throws Exception {
		// Arrange
		createOwner(mockMvc, "Сторінковий Тест Б", "+380501234571");
		createOwner(mockMvc, "Сторінковий Тест А", "+380501234572");
		createOwner(mockMvc, "Сторінковий Тест В", "+380501234573");

		// Act
		var response = listOwners("q", "Сторінковий Тест", "page", "2", "pageSize", "1");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(2)))
				.andExpect(jsonPath("$.pageSize", equalTo(1)))
				.andExpect(jsonPath("$.total", equalTo(3)))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].fullName", equalTo("Сторінковий Тест Б")));
	}

	@Test
	@DisplayName("AD-15.4: сторінка за межами результату — 200 з порожнім items і незмінним total")
	void pageBeyondResult_returnsEmptyItems() throws Exception {
		// Arrange
		createOwner(mockMvc, "Остання Сторінка Власник", "+380501234574");

		// Act
		var response = listOwners("q", "Остання Сторінка", "page", "999");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(999)))
				.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items", empty()));
	}

	@Test
	@DisplayName("AD-10: елемент списку містить id, fullName, phone, email, address, createdAt, updatedAt")
	void listItem_containsAllOwnerFields() throws Exception {
		// Arrange
		createOwner(mockMvc, "Поля Елемента Власник", "+380501234575");

		// Act
		var response = listOwners("q", "Поля Елемента");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").exists())
				.andExpect(jsonPath("$.items[0].fullName", equalTo("Поля Елемента Власник")))
				.andExpect(jsonPath("$.items[0].phone", equalTo("+380501234575")))
				.andExpect(jsonPath("$.items[0].email").hasJsonPath())
				.andExpect(jsonPath("$.items[0].address").hasJsonPath())
				.andExpect(jsonPath("$.items[0].createdAt").exists())
				.andExpect(jsonPath("$.items[0].updatedAt").exists());
	}
}
