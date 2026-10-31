package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestNotifications.notificationsOf;
import static ua.vetclinic.api.TestNotifications.withdraw;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-NF-006, AD-22.1: автоматичне створення сповіщення — системна подія, прочитання — дія адміністратора. */
@ApiIntegrationTest
class NotificationAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	private Doctor doctor;

	@BeforeEach
	void createDoctorAndStartCapture() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Аудиту Сповіщень");
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	@Test
	@DisplayName("AD-22.1: автоматичне створення сповіщення — системна подія LOW_STOCK_NOTIFICATION_CREATED без виконавця-людини")
	void notificationCreation_isAuditedAsSystemEvent() throws Exception {
		// Arrange
		String medicationName = uniqueName("Аудит сповіщення");
		String medicationId = createMedication(mockMvc, medicationName, 5, 10);

		// Act
		withdraw(mockMvc, doctor, medicationId, 1, "2026-12-01", "10:00");

		// Assert
		String notificationId = (String) notificationsOf(mockMvc, medicationId).get(0).get("id");
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=LOW_STOCK_NOTIFICATION_CREATED"),
				containsString("actorId=null"),
				containsString("actorRole=SYSTEM"),
				containsString("entityType=NOTIFICATION"),
				containsString("entityId=" + notificationId),
				containsString("outcome=SUCCESS"))));
		assertThat(audit.lines(), everyItem(not(containsString(medicationName))));
	}

	@Test
	@DisplayName("AD-22.1: сповіщення не створюється — немає й події створення")
	void noNotification_noCreationEvent() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Аудит без сповіщення"), 50, 10);

		// Act
		withdraw(mockMvc, doctor, medicationId, 1, "2026-12-01", "11:00");

		// Assert
		assertThat(audit.lines(), everyItem(not(containsString("LOW_STOCK_NOTIFICATION_CREATED"))));
	}

	@Test
	@DisplayName("REQ-NF-006: позначення прочитаним — подія NOTIFICATION_MARKED_READ з адміністратором і сутністю")
	void markRead_isAudited() throws Exception {
		// Arrange
		String adminEmail = "audit.notifications." + System.nanoTime() + "@clinic.example";
		String adminId = createUser(mockMvc, adminEmail, "Адмін Аудиту Сповіщень", "ADMIN", "Admin-Passw0rd-2026");
		String adminToken = loginAndGetToken(mockMvc, adminEmail, "Admin-Passw0rd-2026");
		String medicationId = createMedication(mockMvc, uniqueName("Аудит прочитання"), 5, 10);
		withdraw(mockMvc, doctor, medicationId, 1, "2026-12-01", "12:00");
		String notificationId = (String) notificationsOf(mockMvc, medicationId).get(0).get("id");

		// Act
		var result = mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/mark-read")
				.header("Authorization", "Bearer " + adminToken))
				.andExpect(status().isOk()).andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=NOTIFICATION_MARKED_READ"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=NOTIFICATION"),
				containsString("entityId=" + notificationId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.1: перегляд списку сповіщень не фіксується в журналі аудиту")
	void listingNotifications_isNotAudited() throws Exception {
		// Arrange
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(get("/api/v1/notifications")
				.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
						.user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}
}
