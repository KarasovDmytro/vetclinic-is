package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestVisits.DEFAULT_VISIT_BODY;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.postVisit;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.TestVisits.Doctor;

/**
 * REQ-F-049, AD-25.7: запис, що вже стоїть на вихідному дні (створений до введення вимоги), лишається чинним.
 * Такий запис не можна створити через API, тому його вставляють у базу напряму.
 */
@ApiIntegrationTest
@Transactional
class LegacyWeekendAppointmentTest {

	private static final String LEGACY_DAY = "2026-10-31";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Doctor doctor;

	private String appointmentId;

	@BeforeEach
	void insertAppointmentOnSaturday() throws Exception {
		doctor = newDoctor(mockMvc, "Вихідні Спадок Лікар");
		String patientId = insertPatient();
		appointmentId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO appointments (id, doctor_id, patient_id, starts_at, ends_at, status, visit_id, created_at,
				                          updated_at)
				VALUES (?::uuid, ?::uuid, ?::uuid, ?::timestamptz, ?::timestamptz, 'SCHEDULED', NULL, now(), now())
				""", appointmentId, doctor.id(), patientId, slot(LEGACY_DAY, "10:00"), slot(LEGACY_DAY, "10:30"));
	}

	private String insertPatient() {
		String ownerId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO owners (id, full_name, phone, email, address, created_at, updated_at)
				VALUES (?::uuid, 'Власник Спадку', '+380501234698', NULL, NULL, now(), now())
				""", ownerId);
		String patientId = UUID.randomUUID().toString();
		jdbcTemplate.update("""
				INSERT INTO patients (id, owner_id, name, species, breed, sex, birth_date, status, created_at, updated_at)
				VALUES (?::uuid, ?::uuid, 'Пацієнт Спадку', 'Кіт', NULL, 'UNKNOWN', NULL, 'ACTIVE', now(), now())
				""", patientId, ownerId);
		return patientId;
	}

	private ResultActions weekendSchedule() throws Exception {
		return mockMvc.perform(get("/api/v1/doctors/" + doctor.id() + "/schedule")
				.param("from", LEGACY_DAY).param("to", "2026-11-01")
				.with(user("admin").roles("ADMIN")));
	}

	private ResultActions reschedule(String startsAt) throws Exception {
		return mockMvc.perform(patch("/api/v1/appointments/" + appointmentId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"startsAt\": \"" + startsAt + "\" }"));
	}

	@Test
	@DisplayName("AD-25.7: запис на вихідному дні лишається в розкладі — єдиний слот суботи зайнятий (BOOKED), вільних немає")
	void legacyAppointment_isOnlySlotOfWeekend() throws Exception {
		// Arrange
		String expectedStart = slot(LEGACY_DAY, "10:00");

		// Act
		var response = weekendSchedule();

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(1)))
				.andExpect(jsonPath("$.slots[0].startsAt", equalTo(expectedStart)))
				.andExpect(jsonPath("$.slots[0].status", equalTo("BOOKED")))
				.andExpect(jsonPath("$.slots[0].appointment.id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.slots[0].appointment.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-25.7: запис на вихідному дні доступний через GET зі статусом SCHEDULED")
	void legacyAppointment_isReturnedByGet() throws Exception {
		// Arrange
		String url = "/api/v1/appointments/" + appointmentId;

		// Act
		var response = mockMvc.perform(get(url).with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.startsAt", equalTo(slot(LEGACY_DAY, "10:00"))))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-25.7: запис на вихідному дні можна скасувати — 200, статус CANCELLED")
	void legacyAppointment_canBeCancelled() throws Exception {
		// Arrange
		String url = "/api/v1/appointments/" + appointmentId + "/cancel";

		// Act
		var response = mockMvc.perform(post(url).with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("CANCELLED")));
	}

	@Test
	@DisplayName("AD-25.7: після скасування запису на вихідному дні слотів у сітці цього діапазону не лишається")
	void afterCancel_weekendScheduleIsEmpty() throws Exception {
		// Arrange
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = weekendSchedule();

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", empty()));
	}

	@Test
	@DisplayName("AD-25.7, AD-14: лікар зберігає прийом за записом на вихідному дні — 201, visitDate дорівнює startsAt")
	void doctorCanSaveVisit_forLegacyAppointment() throws Exception {
		// Arrange
		String expectedVisitDate = slot(LEGACY_DAY, "10:00");

		// Act
		var response = postVisit(mockMvc, doctor, appointmentId, DEFAULT_VISIT_BODY);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.appointmentId", equalTo(appointmentId)))
				.andExpect(jsonPath("$.visitDate", equalTo(expectedVisitDate)));
	}

	@Test
	@DisplayName("AD-25.7, AD-03.2: завершений запис на вихідному дні лишається в сітці — слот BOOKED, статус запису COMPLETED")
	void completedLegacyAppointment_stillOccupiesSlot() throws Exception {
		// Arrange
		postVisit(mockMvc, doctor, appointmentId, DEFAULT_VISIT_BODY).andExpect(status().isCreated());

		// Act
		var response = weekendSchedule();

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(1)))
				.andExpect(jsonPath("$.slots[0].status", equalTo("BOOKED")))
				.andExpect(jsonPath("$.slots[0].appointment.status", equalTo("COMPLETED")));
	}

	@Test
	@DisplayName("AD-25.7: запис на вихідному дні не можна перенести на інший вихідний (01.11.2026, 10:00) — 422 NON_WORKING_DAY")
	void legacyAppointment_cannotMoveToAnotherWeekendDay() throws Exception {
		// Arrange
		String sundayStart = slot("2026-11-01", "10:00");

		// Act
		var response = reschedule(sundayStart);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("NON_WORKING_DAY")));
	}

	@Test
	@DisplayName("AD-25.7: запис на вихідному дні можна перенести на робочий день (30.10.2026, 14:00) — 200")
	void legacyAppointment_canMoveToWorkingDay() throws Exception {
		// Arrange
		String fridayStart = slot("2026-10-30", "14:00");

		// Act
		var response = reschedule(fridayStart);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.startsAt", equalTo(fridayStart)))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}
}
