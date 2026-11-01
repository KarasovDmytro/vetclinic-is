package ua.vetclinic.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestOwners.createOwner;
import static ua.vetclinic.api.TestPatients.createPatient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Спільні кроки Arrange для тестів розкладу: лікарі, пацієнти та записи на прийом. */
public final class TestAppointments {

	private static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Kyiv");

	private TestAppointments() {
	}

	/** Початок слота в часовому поясі клініки у форматі ISO 8601 із зсувом, наприклад 2026-12-01T10:00:00+02:00. */
	public static String slot(String date, String time) {
		ZonedDateTime start = ZonedDateTime.of(LocalDate.parse(date), LocalTime.parse(time), CLINIC_ZONE);
		return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(start.toOffsetDateTime());
	}

	/** Створює нового активного лікаря з унікальним email і повертає його id. */
	public static String createDoctor(MockMvc mockMvc, String fullName) throws Exception {
		return createUser(mockMvc, "doctor." + UUID.randomUUID() + "@clinic.example", fullName, "VETERINARIAN");
	}

	/** Створює нового власника та його пацієнта і повертає id пацієнта. */
	public static String createPatientWithOwner(MockMvc mockMvc, String patientName) throws Exception {
		String phone = "+38050" + String.format("%07d", Math.abs(UUID.randomUUID().getLeastSignificantBits()) % 10_000_000);
		String ownerId = createOwner(mockMvc, "Власник " + patientName, phone);
		return createPatient(mockMvc, ownerId, patientName, "Кіт");
	}

	public static String createAppointment(MockMvc mockMvc, String doctorId, String patientId, String startsAt)
			throws Exception {
		String requestBody = """
				{ "doctorId": "%s", "patientId": "%s", "startsAt": "%s" }
				""".formatted(doctorId, patientId, startsAt);
		var created = mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	public static void cancelAppointment(MockMvc mockMvc, String appointmentId) throws Exception {
		mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/cancel")
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}
}
