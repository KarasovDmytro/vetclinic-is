package ua.vetclinic.api.notifications;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestNotifications.markRead;
import static ua.vetclinic.api.TestNotifications.notificationsOf;
import static ua.vetclinic.api.TestNotifications.withdraw;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;

import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-047, AD-19.2: перегляд сповіщень адміністратором (операція listNotifications). */
@ApiIntegrationTest
class ListNotificationsTest {

	@Autowired
	private MockMvc mockMvc;

	private Doctor doctor;

	@BeforeEach
	void createDoctor() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Списку Сповіщень");
	}

	private MockHttpServletRequestBuilder listAsAdmin() {
		return get("/api/v1/notifications").with(user("admin").roles("ADMIN"));
	}

	private String lowStockNotification(String namePrefix, String time) throws Exception {
		String medicationId = createMedication(mockMvc, uniqueName(namePrefix), 5, 10);
		withdraw(mockMvc, doctor, medicationId, 1, "2026-11-27", time);
		return (String) notificationsOf(mockMvc, medicationId).get(0).get("id");
	}

	@Test
	@DisplayName("REQ-F-047: адміністратор отримує сторінку сповіщень — 200 з page, pageSize, total та items")
	void adminListsNotifications_returnsPage() throws Exception {
		// Arrange
		String notificationId = lowStockNotification("Список", "10:00");

		// Act
		var response = mockMvc.perform(listAsAdmin());

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)))
				.andExpect(jsonPath("$.items[*].id", hasItem(notificationId)));
		String body = mockMvc.perform(listAsAdmin()).andReturn().getResponse().getContentAsString();
		assertThat((Integer) JsonPath.read(body, "$.total"), greaterThanOrEqualTo(1));
	}

	@Test
	@DisplayName("REQ-F-031: лікар не має доступу до сповіщень — 403 FORBIDDEN")
	void veterinarian_returns403() throws Exception {
		// Arrange
		var request = get("/api/v1/notifications").header("Authorization", doctor.bearer());

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /notifications без токена — 401 UNAUTHORIZED")
	void withoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/notifications");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-19.2: сповіщення відсортовані за createdAt за спаданням — найновіше першим")
	void notifications_areSortedByCreatedAtDescending() throws Exception {
		// Arrange
		String older = lowStockNotification("Старіше", "10:00");
		String newer = lowStockNotification("Новіше", "11:00");

		// Act
		String body = mockMvc.perform(listAsAdmin().param("pageSize", "100")).andReturn().getResponse()
				.getContentAsString();

		// Assert
		List<String> ids = JsonPath.read(body, "$.items[*].id");
		assertThat(ids.indexOf(newer), lessThanOrEqualTo(ids.indexOf(older) - 1));
	}

	@Test
	@DisplayName("REQ-F-047: фільтр read=false повертає лише непрочитані, read=true — лише прочитані")
	void readFilter_separatesReadAndUnread() throws Exception {
		// Arrange
		String unread = lowStockNotification("Непрочитане", "10:00");
		String read = lowStockNotification("Прочитане", "11:00");
		markRead(mockMvc, read);

		// Act
		var unreadResponse = mockMvc.perform(listAsAdmin().param("read", "false").param("pageSize", "100"));
		var readResponse = mockMvc.perform(listAsAdmin().param("read", "true").param("pageSize", "100"));

		// Assert
		unreadResponse.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", hasItem(unread)))
				.andExpect(jsonPath("$.items[*].id", not(hasItem(read))))
				.andExpect(jsonPath("$.items[*].read", everyItem(equalTo(false))));
		readResponse.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", hasItem(read)))
				.andExpect(jsonPath("$.items[*].id", not(hasItem(unread))))
				.andExpect(jsonPath("$.items[*].read", everyItem(equalTo(true))));
	}

	@Test
	@DisplayName("AD-18.2: read не boolean — 400 MALFORMED_REQUEST")
	void malformedRead_returns400() throws Exception {
		// Arrange
		var request = listAsAdmin().param("read", "можливо");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-19.2: пагінація по одному елементу — перша й друга сторінки містять різні сповіщення")
	void paging_returnsDifferentItemsOnDifferentPages() throws Exception {
		// Arrange
		lowStockNotification("Сторінка перша", "10:00");
		lowStockNotification("Сторінка друга", "11:00");

		// Act
		String first = mockMvc.perform(listAsAdmin().param("page", "1").param("pageSize", "1")).andReturn()
				.getResponse().getContentAsString();
		String second = mockMvc.perform(listAsAdmin().param("page", "2").param("pageSize", "1")).andReturn()
				.getResponse().getContentAsString();

		// Assert
		List<String> firstIds = JsonPath.read(first, "$.items[*].id");
		List<String> secondIds = JsonPath.read(second, "$.items[*].id");
		assertThat(firstIds, hasSize(1));
		assertThat(secondIds, hasSize(1));
		assertThat(firstIds, not(contains(secondIds.get(0))));
		assertThat((Integer) JsonPath.read(second, "$.page"), equalTo(2));
		assertThat((Integer) JsonPath.read(second, "$.pageSize"), equalTo(1));
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
}
