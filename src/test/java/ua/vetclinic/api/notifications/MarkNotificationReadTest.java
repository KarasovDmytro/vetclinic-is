package ua.vetclinic.api.notifications;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestData.CLINIC_OFFSET_TIMESTAMP;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestNotifications.notificationsOf;
import static ua.vetclinic.api.TestNotifications.withdraw;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import org.springframework.test.web.servlet.ResultActions;

/** REQ-F-047, AD-16.2, AD-20.2: позначення сповіщення прочитаним (операція markNotificationRead). */
@ApiIntegrationTest
class MarkNotificationReadTest {

	@Autowired
	private MockMvc mockMvc;

	private String medicationId;

	private String notificationId;

	@BeforeEach
	void createNotification() throws Exception {
		Doctor doctor = newDoctor(mockMvc, "Лікар Прочитання");
		medicationId = createMedication(mockMvc, uniqueName("Прочитання"), 5, 10);
		withdraw(mockMvc, doctor, medicationId, 1, "2026-11-28", "10:00");
		notificationId = (String) notificationsOf(mockMvc, medicationId).get(0).get("id");
	}

	private ResultActions markAsAdmin(String id) throws Exception {
		return mockMvc.perform(post("/api/v1/notifications/" + id + "/mark-read").with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("REQ-F-047: адміністратор позначає сповіщення прочитаним — 200, read true, решта полів без змін")
	void adminMarksRead_returns200() throws Exception {
		// Arrange
		String id = notificationId;

		// Act
		var response = markAsAdmin(id);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(id)))
				.andExpect(jsonPath("$.type", equalTo("LOW_STOCK")))
				.andExpect(jsonPath("$.medicationId", equalTo(medicationId)))
				.andExpect(jsonPath("$.quantity", equalTo(4)))
				.andExpect(jsonPath("$.minThreshold", equalTo(10)))
				.andExpect(jsonPath("$.read", equalTo(true)))
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)));
	}

	@Test
	@DisplayName("AD-16.2: повторне позначення ідемпотентне — другий виклик також 200 з read true")
	void markingTwice_returns200Both() throws Exception {
		// Arrange
		markAsAdmin(notificationId).andExpect(status().isOk());

		// Act
		var response = markAsAdmin(notificationId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.read", equalTo(true)));
	}

	@Test
	@DisplayName("AD-20.2: прапорець спільний для всіх адміністраторів — після прочитання одним інший бачить read true")
	void readFlag_isSharedBetweenAdmins() throws Exception {
		// Arrange
		markAsAdmin(notificationId).andExpect(status().isOk());

		// Act
		var notifications = notificationsOf(mockMvc, medicationId);

		// Assert
		assertThat(notifications.get(0).get("read"), equalTo(true));
	}

	@Test
	@DisplayName("AD-18.2: сповіщення з таким id немає — 404 NOT_FOUND")
	void unknownNotification_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000b0a";

		// Act
		var response = markAsAdmin(unknownId);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.2: id сповіщення не UUID — 400 MALFORMED_REQUEST")
	void malformedNotificationId_returns400() throws Exception {
		// Arrange
		String malformedId = "не-uuid";

		// Act
		var response = markAsAdmin(malformedId);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-031: лікар не може позначати сповіщення — 403 FORBIDDEN, сповіщення лишається непрочитаним")
	void veterinarian_returns403() throws Exception {
		// Arrange
		var request = post("/api/v1/notifications/" + notificationId + "/mark-read")
				.with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		assertThat(notificationsOf(mockMvc, medicationId).get(0).get("read"), equalTo(false));
	}

	@Test
	@DisplayName("REQ-NF-002: mark-read без токена — 401 UNAUTHORIZED")
	void withoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/notifications/" + notificationId + "/mark-read");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-19.4: GET /notifications/{id}/mark-read — 405 METHOD_NOT_ALLOWED, дія виконується лише через POST")
	void getMarkRead_returns405() throws Exception {
		// Arrange
		var request = get("/api/v1/notifications/" + notificationId + "/mark-read")
				.with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed());
	}
}
