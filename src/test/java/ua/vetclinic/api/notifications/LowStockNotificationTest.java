package ua.vetclinic.api.notifications;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestData.CLINIC_OFFSET_TIMESTAMP;
import static ua.vetclinic.api.TestData.UUID_PATTERN;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestNotifications.markRead;
import static ua.vetclinic.api.TestNotifications.notificationsOf;
import static ua.vetclinic.api.TestNotifications.withdraw;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.postVisit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-047, AD-02: сповіщення LOW_STOCK створюються під час збереження прийому (операція createVisit). */
@ApiIntegrationTest
class LowStockNotificationTest {

	@Autowired
	private MockMvc mockMvc;

	private Doctor doctor;

	@BeforeEach
	void createDoctor() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Сповіщень");
	}

	@Test
	@DisplayName("REQ-F-047, AD-02.2: залишок після списання строго нижчий за поріг — створюється непрочитане сповіщення LOW_STOCK")
	void quantityBelowThreshold_createsNotification() throws Exception {
		// Arrange
		String name = uniqueName("Новокаїн сповіщення");
		String medicationId = createMedication(mockMvc, name, 12, 10);

		// Act
		withdraw(mockMvc, doctor, medicationId, 7, "2026-11-26", "10:00");

		// Assert
		var notifications = notificationsOf(mockMvc, medicationId);
		assertThat(notifications, hasSize(1));
		var notification = notifications.get(0);
		assertThat((String) notification.get("id"), matchesPattern(UUID_PATTERN));
		assertThat(notification.get("type"), equalTo("LOW_STOCK"));
		assertThat(notification.get("medicationId"), equalTo(medicationId));
		assertThat(notification.get("medicationName"), equalTo(name));
		assertThat(notification.get("quantity"), equalTo(5));
		assertThat(notification.get("minThreshold"), equalTo(10));
		assertThat(notification.get("read"), equalTo(false));
		assertThat((String) notification.get("createdAt"), matchesPattern(CLINIC_OFFSET_TIMESTAMP));
	}

	@Test
	@DisplayName("AD-02.2: залишок після списання дорівнює порогу — сповіщення немає (поріг строгий)")
	void quantityEqualToThreshold_createsNothing() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Рівно порогу"), 15, 10);

		// Act
		withdraw(mockMvc, doctor, medicationId, 5, "2026-11-26", "10:00");

		// Assert
		assertThat(notificationsOf(mockMvc, medicationId), empty());
	}

	@Test
	@DisplayName("AD-02.2: залишок після списання вищий за поріг — сповіщення немає")
	void quantityAboveThreshold_createsNothing() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Вище порогу"), 50, 10);

		// Act
		withdraw(mockMvc, doctor, medicationId, 5, "2026-11-26", "10:00");

		// Assert
		assertThat(notificationsOf(mockMvc, medicationId), empty());
	}

	@Test
	@DisplayName("AD-02.4: поріг не заданий — сповіщення не створюються, навіть коли залишок дійшов до нуля")
	void withoutThreshold_createsNothing() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Без порогу"), 5, null);

		// Act
		withdraw(mockMvc, doctor, medicationId, 5, "2026-11-26", "10:00");

		// Assert
		assertThat(notificationsOf(mockMvc, medicationId), empty());
	}

	@Test
	@DisplayName("AD-02.3: поки існує непрочитане сповіщення, нове для цього препарату не створюється")
	void unreadNotificationExists_noDuplicate() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Без дублювання"), 12, 10);
		withdraw(mockMvc, doctor, medicationId, 3, "2026-11-26", "10:00");

		// Act
		withdraw(mockMvc, doctor, medicationId, 4, "2026-11-26", "11:00");

		// Assert
		var notifications = notificationsOf(mockMvc, medicationId);
		assertThat(notifications, hasSize(1));
		assertThat(notifications.get(0).get("quantity"), equalTo(9));
	}

	@Test
	@DisplayName("AD-02.3: після прочитання сповіщення наступне падіння нижче порогу створює нове")
	void afterMarkedRead_newNotificationIsCreated() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Після прочитання"), 12, 10);
		withdraw(mockMvc, doctor, medicationId, 3, "2026-11-26", "10:00");
		markRead(mockMvc, (String) notificationsOf(mockMvc, medicationId).get(0).get("id"));

		// Act
		withdraw(mockMvc, doctor, medicationId, 2, "2026-11-26", "11:00");

		// Assert
		var notifications = notificationsOf(mockMvc, medicationId);
		assertThat(notifications, hasSize(2));
		assertThat(notifications.get(0).get("read"), equalTo(false));
		assertThat(notifications.get(0).get("quantity"), equalTo(7));
		assertThat(notifications.get(1).get("read"), equalTo(true));
	}

	@Test
	@DisplayName("AD-02.5: ручне коригування залишку нижче порогу сповіщення не створює")
	void manualAdjustment_createsNothing() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Ручне коригування"), 12, 10);

		// Act
		mockMvc.perform(post("/api/v1/medications/" + medicationId + "/adjust-stock")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"delta\": -10 }"))
				.andExpect(status().isOk());

		// Assert
		assertThat(notificationsOf(mockMvc, medicationId), empty());
	}

	@Test
	@DisplayName("REQ-F-046: відхилений через нестачу прийом не створює сповіщень про інші препарати")
	void rejectedVisit_createsNothing() throws Exception {
		// Arrange
		String lowAfter = createMedication(mockMvc, uniqueName("Мало б стати"), 12, 10);
		String lacking = createMedication(mockMvc, uniqueName("Бракує"), 1, null);
		String patientId = createPatientWithOwner(mockMvc, "Пацієнт Відхиленого Прийому");
		String appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-11-26", "12:00"));
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + lowAfter
				+ "\", \"quantity\": 9 }, { \"medicationId\": \"" + lacking + "\", \"quantity\": 5 } ] }";

		// Act
		postVisit(mockMvc, doctor, appointmentId, requestBody).andExpect(status().isConflict());

		// Assert
		assertThat(notificationsOf(mockMvc, lowAfter), empty());
	}

	@Test
	@DisplayName("REQ-F-047: один прийом знижує кілька препаратів нижче порогу — сповіщення для кожного")
	void severalMedicationsBelowThreshold_createNotificationEach() throws Exception {
		// Arrange
		String first = createMedication(mockMvc, uniqueName("Перший низький"), 10, 8);
		String second = createMedication(mockMvc, uniqueName("Другий низький"), 10, 8);
		String patientId = createPatientWithOwner(mockMvc, "Пацієнт Двох Препаратів");
		String appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-11-26", "13:00"));
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + first
				+ "\", \"quantity\": 5 }, { \"medicationId\": \"" + second + "\", \"quantity\": 6 } ] }";

		// Act
		postVisit(mockMvc, doctor, appointmentId, requestBody).andExpect(status().isCreated());

		// Assert
		assertThat(notificationsOf(mockMvc, first), hasSize(1));
		assertThat(notificationsOf(mockMvc, second), hasSize(1));
	}

	@Test
	@DisplayName("AD-02.2: прийом без препаратів не створює сповіщень")
	void visitWithoutMedications_createsNothing() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Не використаний"), 1, 10);
		String patientId = createPatientWithOwner(mockMvc, "Пацієнт Без Препаратів");
		String appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-11-26", "14:00"));

		// Act
		postVisit(mockMvc, doctor, appointmentId, "{ \"diagnosis\": \"Діагноз\" }").andExpect(status().isCreated());

		// Assert
		assertThat(notificationsOf(mockMvc, medicationId), empty());
	}

	@Test
	@DisplayName("REQ-F-047: препарат уже був нижче порогу до прийому — списання ще нижче створює сповіщення, якщо непрочитаних немає")
	void alreadyLowStock_createsNotificationOnNextWithdrawal() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Уже низький"), 6, 10);

		// Act
		withdraw(mockMvc, doctor, medicationId, 1, "2026-11-26", "15:00");

		// Assert
		var notifications = notificationsOf(mockMvc, medicationId);
		assertThat(notifications, hasSize(1));
		assertThat(notifications.get(0).get("quantity"), equalTo(5));
	}

	@Test
	@DisplayName("REQ-F-047: після списання до нуля сповіщення фіксує залишок 0")
	void withdrawnToZero_notificationHasZeroQuantity() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("До нуля сповіщення"), 3, 2);

		// Act
		withdraw(mockMvc, doctor, medicationId, 3, "2026-11-26", "16:00");

		// Assert
		var notifications = notificationsOf(mockMvc, medicationId);
		assertThat(notifications, hasSize(1));
		assertThat(notifications.get(0).get("quantity"), equalTo(0));
	}
}
