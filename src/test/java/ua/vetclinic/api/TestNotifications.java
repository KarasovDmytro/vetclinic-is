package ua.vetclinic.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestVisits.createVisit;
import static ua.vetclinic.api.TestVisits.visitBodyWith;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import org.springframework.test.web.servlet.MockMvc;

/** Спільні кроки Arrange для тестів сповіщень про низький залишок. */
public final class TestNotifications {

	private TestNotifications() {
	}

	/** Зберігає прийом, що списує вказану кількість препарату, і повертає id прийому. */
	public static String withdraw(MockMvc mockMvc, TestVisits.Doctor doctor, String medicationId, int quantity,
			String date, String time) throws Exception {
		String patientId = createPatientWithOwner(mockMvc, "Пацієнт Сповіщення " + date + time);
		String appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot(date, time));
		return createVisit(mockMvc, doctor, appointmentId, visitBodyWith(medicationId, quantity));
	}

	/** Сповіщення про вказаний препарат (найновіші першими). */
	public static List<Map<String, Object>> notificationsOf(MockMvc mockMvc, String medicationId) throws Exception {
		String body = mockMvc.perform(get("/api/v1/notifications").param("pageSize", "100")
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.items[?(@.medicationId == '" + medicationId + "')]");
	}

	public static void markRead(MockMvc mockMvc, String notificationId) throws Exception {
		mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/mark-read")
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}
}
