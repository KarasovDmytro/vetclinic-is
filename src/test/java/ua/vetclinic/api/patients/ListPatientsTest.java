package ua.vetclinic.api.patients;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;
import static ua.vetclinic.api.TestPatients.createPatient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-017, AD-15, AD-17.8: пошук і список пацієнтів (операція listPatients). */
@ApiIntegrationTest
class ListPatientsTest {

	@Autowired
	private MockMvc mockMvc;

	private ResultActions listPatients(String role, String... paramsNameValue) throws Exception {
		var request = get("/api/v1/patients").with(user("someone").roles(role));
		for (int i = 0; i < paramsNameValue.length; i += 2) {
			request.param(paramsNameValue[i], paramsNameValue[i + 1]);
		}
		return mockMvc.perform(request);
	}

	private void archive(String patientId) throws Exception {
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("REQ-F-017: адміністратор отримує першу сторінку пацієнтів — 200 з page, pageSize, total, items")
	void adminListsPatients_returns200WithDefaultPage() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Списку", "+380501234620");
		createPatient(mockMvc, ownerId, "Список Перший", "Кіт");
		createPatient(mockMvc, ownerId, "Список Другий", "Собака");

		// Act
		var response = listPatients("ADMIN");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)))
				.andExpect(jsonPath("$.total", greaterThanOrEqualTo(2)))
				.andExpect(jsonPath("$.items[0].id").exists())
				.andExpect(jsonPath("$.items[0].ownerId").exists())
				.andExpect(jsonPath("$.items[0].name").exists())
				.andExpect(jsonPath("$.items[0].species").exists())
				.andExpect(jsonPath("$.items[0].sex").exists())
				.andExpect(jsonPath("$.items[0].status").exists());
	}

	@Test
	@DisplayName("REQ-F-017: лікар теж бачить список пацієнтів — 200")
	void veterinarianListsPatients_returns200() throws Exception {
		// Arrange
		String role = "VETERINARIAN";

		// Act
		var response = listPatients(role);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-15: параметр q шукає підрядок в імені пацієнта без урахування регістру")
	void searchByQuery_returnsPatientsWithSubstringIgnoringCase() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Пошуку", "+380501234621");
		createPatient(mockMvc, ownerId, "Барсик Рудий", "Кіт");
		createPatient(mockMvc, ownerId, "Шарик Чорний", "Собака");

		// Act
		var response = listPatients("VETERINARIAN", "q", "АРСИК");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", hasItem("Барсик Рудий")))
				.andExpect(jsonPath("$.items[*].name", everyItem(containsStringIgnoringCase("арсик"))))
				.andExpect(jsonPath("$.items[*].name", not(hasItem("Шарик Чорний"))));
	}

	@Test
	@DisplayName("AD-15: параметр q з одного символу — 422, поле q, причина TOO_SHORT")
	void searchWithOneCharacter_returns422TooShort() throws Exception {
		// Arrange
		String query = "б";

		// Act
		var response = listPatients("ADMIN", "q", query);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("q")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.8: параметр q довший за 100 символів — 422, поле q, причина TOO_LONG")
	void searchWithTooLongQuery_returns422TooLong() throws Exception {
		// Arrange
		String query = "б".repeat(101);

		// Act
		var response = listPatients("ADMIN", "q", query);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("q")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-15: пошук без збігів — 200, total 0 і порожній items")
	void queryWithoutMatches_returnsEmptyPage() throws Exception {
		// Arrange
		String query = "Неіснуючеім'я-ЯЯЯ";

		// Act
		var response = listPatients("ADMIN", "q", query);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)))
				.andExpect(jsonPath("$.items", empty()));
	}

	@Test
	@DisplayName("AD-15.2: символи % та _ у q шукаються буквально, а не як шаблон")
	void wildcardCharactersInQuery_areLiteral() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Шаблону", "+380501234622");
		createPatient(mockMvc, ownerId, "Шаблонний Пацієнт", "Кіт");

		// Act
		var response = listPatients("ADMIN", "q", "%%");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)));
	}

	@Test
	@DisplayName("REQ-F-017: фільтр ownerId повертає лише пацієнтів цього власника")
	void filterByOwner_returnsOnlyThatOwnersPatients() throws Exception {
		// Arrange
		String ownerA = createOwner(mockMvc, "Власник А Фільтр", "+380501234623");
		String ownerB = createOwner(mockMvc, "Власник Б Фільтр", "+380501234624");
		String patientA1 = createPatient(mockMvc, ownerA, "Фільтр Один", "Кіт");
		String patientA2 = createPatient(mockMvc, ownerA, "Фільтр Два", "Кіт");
		createPatient(mockMvc, ownerB, "Фільтр Чужий", "Кіт");

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerA);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(2)))
				.andExpect(jsonPath("$.items[*].id", containsInAnyOrder(patientA1, patientA2)))
				.andExpect(jsonPath("$.items[*].ownerId", everyItem(equalTo(ownerA))));
	}

	@Test
	@DisplayName("REQ-F-017: q і ownerId поєднуються — повертаються пацієнти, що задовольняють обидві умови")
	void queryAndOwnerFilter_areCombined() throws Exception {
		// Arrange
		String ownerA = createOwner(mockMvc, "Власник Комбінації А", "+380501234625");
		String ownerB = createOwner(mockMvc, "Власник Комбінації Б", "+380501234626");
		String wanted = createPatient(mockMvc, ownerA, "Комбо Рекс", "Собака");
		createPatient(mockMvc, ownerA, "Інший Кіт", "Кіт");
		createPatient(mockMvc, ownerB, "Комбо Бакс", "Собака");

		// Act
		var response = listPatients("ADMIN", "q", "комбо", "ownerId", ownerA);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", contains(wanted)));
	}

	@Test
	@DisplayName("REQ-F-017: фільтр ownerId неіснуючого власника — 200 з порожнім результатом")
	void filterByUnknownOwner_returnsEmptyPage() throws Exception {
		// Arrange
		String unknownOwnerId = "00000000-0000-4000-8000-000000000531";

		// Act
		var response = listPatients("ADMIN", "ownerId", unknownOwnerId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)));
	}

	@Test
	@DisplayName("AD-18.2: ownerId у фільтрі не UUID — 400 MALFORMED_REQUEST")
	void filterByInvalidOwnerId_returns400() throws Exception {
		// Arrange
		String ownerId = "not-a-uuid";

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerId);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-015: фільтр status=ARCHIVED повертає лише архівні картки")
	void filterByArchived_returnsOnlyArchivedPatients() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Статусів", "+380501234627");
		String archived = createPatient(mockMvc, ownerId, "Статус Архівний", "Кіт");
		createPatient(mockMvc, ownerId, "Статус Активний", "Кіт");
		archive(archived);

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerId, "status", "ARCHIVED");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", contains(archived)))
				.andExpect(jsonPath("$.items[*].status", everyItem(equalTo("ARCHIVED"))));
	}

	@Test
	@DisplayName("REQ-F-015: фільтр status=ACTIVE не повертає архівні картки")
	void filterByActive_excludesArchivedPatients() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Активних", "+380501234628");
		String archived = createPatient(mockMvc, ownerId, "Активність Архівний", "Кіт");
		String active = createPatient(mockMvc, ownerId, "Активність Активний", "Кіт");
		archive(archived);

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerId, "status", "ACTIVE");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", contains(active)));
	}

	@Test
	@DisplayName("REQ-F-016: без фільтра статусу список містить і активні, і архівні картки")
	void defaultList_includesArchivedPatients() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Всіх Карток", "+380501234629");
		String archived = createPatient(mockMvc, ownerId, "Усі Архівний", "Кіт");
		String active = createPatient(mockMvc, ownerId, "Усі Активний", "Кіт");
		archive(archived);

		// Act
		var response = listPatients("VETERINARIAN", "ownerId", ownerId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", containsInAnyOrder(archived, active)));
	}

	@Test
	@DisplayName("AD-19.2: пацієнти відсортовані за іменем за зростанням")
	void patients_areSortedByNameAscending() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Сортування", "+380501234630");
		createPatient(mockMvc, ownerId, "Сорт Яблучко", "Кіт");
		createPatient(mockMvc, ownerId, "Сорт Абрикос", "Кіт");
		createPatient(mockMvc, ownerId, "Сорт Мандарин", "Кіт");

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].name", contains("Сорт Абрикос", "Сорт Мандарин", "Сорт Яблучко")));
	}

	@Test
	@DisplayName("AD-15.4: page і pageSize вибирають зріз відсортованого результату, total — загальна кількість")
	void paging_returnsRequestedSliceAndTotal() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Сторінок", "+380501234631");
		createPatient(mockMvc, ownerId, "Сторінка Б", "Кіт");
		createPatient(mockMvc, ownerId, "Сторінка А", "Кіт");
		createPatient(mockMvc, ownerId, "Сторінка В", "Кіт");

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerId, "page", "2", "pageSize", "1");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(2)))
				.andExpect(jsonPath("$.pageSize", equalTo(1)))
				.andExpect(jsonPath("$.total", equalTo(3)))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].name", equalTo("Сторінка Б")));
	}

	@Test
	@DisplayName("AD-15.4: сторінка за межами результату — 200 з порожнім items і незмінним total")
	void pageBeyondResult_returnsEmptyItems() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Власник Останньої", "+380501234632");
		createPatient(mockMvc, ownerId, "Остання Сторінка", "Кіт");

		// Act
		var response = listPatients("ADMIN", "ownerId", ownerId, "page", "999");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items", empty()));
	}

	@Test
	@DisplayName("AD-17.8: pageSize менший за 1 — 422, поле pageSize, причина OUT_OF_RANGE")
	void zeroPageSize_returns422OutOfRange() throws Exception {
		// Arrange
		String pageSize = "0";

		// Act
		var response = listPatients("ADMIN", "pageSize", pageSize);

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
		var response = listPatients("ADMIN", "pageSize", pageSize);

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
		var response = listPatients("ADMIN", "page", page);

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
		var response = listPatients("ADMIN", "page", page);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: невідоме значення status — 400 MALFORMED_REQUEST")
	void invalidStatusFilter_returns400() throws Exception {
		// Arrange
		String status = "DELETED";

		// Act
		var response = listPatients("ADMIN", "status", status);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /patients без токена — 401 UNAUTHORIZED")
	void listWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/patients");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}
}
