package ua.vetclinic.api.appointments;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-020, REQ-F-026, AD-03, AD-05, AD-17.5, AD-20.3: сітка розкладу лікаря (операція getDoctorSchedule). */
@ApiIntegrationTest
class DoctorScheduleTest {

	private static final String OFFSET_TIMESTAMP = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\+0[23]:00";

	@Autowired
	private MockMvc mockMvc;

	private ResultActions schedule(String role, String doctorId, String from, String to) throws Exception {
		return mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", from)
				.param("to", to)
				.with(user("someone").roles(role)));
	}

	@Test
	@DisplayName("REQ-F-020: порожній день — 18 вільних слотів по 30 хвилин з 09:00 до 18:00")
	void emptyDay_hasEighteenFreeSlotsFrom0900To1800() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Порожній День");

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.from", equalTo("2026-12-01")))
				.andExpect(jsonPath("$.to", equalTo("2026-12-01")))
				.andExpect(jsonPath("$.slotMinutes", equalTo(30)))
				.andExpect(jsonPath("$.slots", hasSize(18)))
				.andExpect(jsonPath("$.slots[*].status", everyItem(equalTo("FREE"))))
				.andExpect(jsonPath("$.slots[*].appointment", everyItem(nullValue())))
				.andExpect(jsonPath("$.slots[0].startsAt", equalTo(slot("2026-12-01", "09:00"))))
				.andExpect(jsonPath("$.slots[0].endsAt", equalTo(slot("2026-12-01", "09:30"))))
				.andExpect(jsonPath("$.slots[17].startsAt", equalTo(slot("2026-12-01", "17:30"))))
				.andExpect(jsonPath("$.slots[17].endsAt", equalTo(slot("2026-12-01", "18:00"))));
	}

	@Test
	@DisplayName("AD-19.1: час слотів віддається з зсувом клініки — +03:00 влітку/восени і +02:00 взимку")
	void slotTimes_useClinicOffsetForSummerAndWinterDays() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Зсув");

		// Act
		var autumn = schedule("ADMIN", doctorId, "2026-10-20", "2026-10-20");
		var winter = schedule("ADMIN", doctorId, "2026-12-01", "2026-12-01");

		// Assert
		autumn.andExpect(jsonPath("$.slots[0].startsAt", equalTo("2026-10-20T09:00:00+03:00")));
		winter.andExpect(jsonPath("$.slots[0].startsAt", equalTo("2026-12-01T09:00:00+02:00")));
	}

	@Test
	@DisplayName("REQ-F-020: слот із записом SCHEDULED зайнятий — BOOKED з id запису, пацієнтом, його ім'ям і статусом")
	void scheduledAppointment_marksSlotBooked() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Зайнятий");
		String patientId = createPatientWithOwner(mockMvc, "Барсик Розкладу");
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-02", "14:00"));

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-02", "2026-12-02");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots[10].startsAt", equalTo(slot("2026-12-02", "14:00"))))
				.andExpect(jsonPath("$.slots[10].status", equalTo("BOOKED")))
				.andExpect(jsonPath("$.slots[10].appointment.id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.slots[10].appointment.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.slots[10].appointment.patientName", equalTo("Барсик Розкладу")))
				.andExpect(jsonPath("$.slots[10].appointment.status", equalTo("SCHEDULED")))
				.andExpect(jsonPath("$.slots[9].status", equalTo("FREE")))
				.andExpect(jsonPath("$.slots[11].status", equalTo("FREE")));
	}

	@Test
	@DisplayName("AD-03.1: скасований запис звільняє слот — він знову FREE")
	void cancelledAppointment_freesSlot() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Скасований");
		String patientId = createPatientWithOwner(mockMvc, "Мурка Розкладу");
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-03", "10:00"));
		cancelAppointment(mockMvc, appointmentId);

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-03", "2026-12-03");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots[2].startsAt", equalTo(slot("2026-12-03", "10:00"))))
				.andExpect(jsonPath("$.slots[2].status", equalTo("FREE")))
				.andExpect(jsonPath("$.slots[2].appointment", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-023: той самий час у іншого лікаря лишається вільним")
	void sameTimeForOtherDoctor_remainsFree() throws Exception {
		// Arrange
		String busyDoctor = createDoctor(mockMvc, "Розклад Зайнятий Лікар");
		String freeDoctor = createDoctor(mockMvc, "Розклад Вільний Лікар");
		String patientId = createPatientWithOwner(mockMvc, "Рекс Розкладу");
		createAppointment(mockMvc, busyDoctor, patientId, slot("2026-12-04", "11:00"));

		// Act
		var response = schedule("ADMIN", freeDoctor, "2026-12-04", "2026-12-04");

		// Assert
		response.andExpect(jsonPath("$.slots[4].startsAt", equalTo(slot("2026-12-04", "11:00"))))
				.andExpect(jsonPath("$.slots[4].status", equalTo("FREE")));
	}

	@Test
	@DisplayName("AD-17.5: діапазон із семи днів допустимий — 126 слотів, відсортованих за часом зростання")
	void sevenDayRange_returns126SortedSlots() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Тиждень");

		// Act
		String body = schedule("ADMIN", doctorId, "2026-12-07", "2026-12-13")
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// Assert
		List<String> starts = JsonPath.read(body, "$.slots[*].startsAt");
		assertThat(starts, hasSize(126));
		assertThat(starts.get(0), equalTo(slot("2026-12-07", "09:00")));
		assertThat(starts.get(125), equalTo(slot("2026-12-13", "17:30")));
		List<String> sorted = starts.stream().sorted().toList();
		assertThat(starts, equalTo(sorted));
	}

	@Test
	@DisplayName("AD-17.5: діапазон більший за 7 днів — 422, поле to, причина RANGE_TOO_LARGE")
	void eightDayRange_returns422RangeTooLarge() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Забагато");

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-07", "2026-12-14");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("to")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("RANGE_TOO_LARGE")));
	}

	@Test
	@DisplayName("AD-17.5: from пізніше за to — 422, поле to, причина INVALID_RANGE")
	void fromAfterTo_returns422InvalidRange() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Навпаки");

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-10", "2026-12-09");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("to")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_RANGE")));
	}

	@Test
	@DisplayName("AD-18: параметр from не вказано — 422, поле from, причина REQUIRED")
	void missingFrom_returns422Required() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Без From");

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("to", "2026-12-01").with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("from")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-18: параметр to не вказано — 422, поле to, причина REQUIRED")
	void missingTo_returns422Required() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Без To");

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors/" + doctorId + "/schedule")
				.param("from", "2026-12-01").with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("to")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-18.2: дата не у форматі YYYY-MM-DD — 400 MALFORMED_REQUEST")
	void dateWithWrongFormat_returns400() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Формат");

		// Act
		var response = schedule("ADMIN", doctorId, "01.12.2026", "2026-12-01");

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-020: неіснуючий лікар — 404 NOT_FOUND")
	void unknownDoctor_returns404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000601";

		// Act
		var response = schedule("ADMIN", unknownId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("REQ-F-020: користувач з роллю ADMIN не є лікарем — розклад за його id дає 404")
	void administratorId_returns404() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "schedule.admin." + UUID.randomUUID() + "@clinic.example",
				"Розклад Не Лікар", "ADMIN");

		// Act
		var response = schedule("ADMIN", adminId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("AD-20.1: розклад деактивованого лікаря доступний для перегляду — 200")
	void deactivatedDoctorSchedule_isStillViewable() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Деактивований");
		mockMvc.perform(post("/api/v1/users/" + doctorId + "/deactivate").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(18)));
	}

	@Test
	@DisplayName("REQ-F-026: лікар бачить розклад будь-якого лікаря — 200")
	void veterinarianViewsAnyDoctorSchedule_returns200() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Для Колеги");

		// Act
		var response = schedule("VETERINARIAN", doctorId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.slots", hasSize(18)));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор лікаря не UUID — 400 MALFORMED_REQUEST")
	void doctorIdNotUuid_returns400() throws Exception {
		// Arrange
		String doctorId = "not-a-uuid";

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /doctors/{id}/schedule без токена — 401 UNAUTHORIZED")
	void scheduleWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/doctors/00000000-0000-4000-8000-000000000601/schedule")
				.param("from", "2026-12-01").param("to", "2026-12-01");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — невалідний діапазон для неіснуючого лікаря дає 422, а не 404")
	void invalidRangeForUnknownDoctor_returns422Before404() throws Exception {
		// Arrange
		String unknownId = "00000000-0000-4000-8000-000000000601";

		// Act
		var response = schedule("ADMIN", unknownId, "2026-12-10", "2026-12-01");

		// Assert
		response.andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("REQ-F-020: запис одного дня не позначає зайнятими слоти інших днів діапазону")
	void appointmentMarksOnlyItsOwnDay() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Два Дні");
		String patientId = createPatientWithOwner(mockMvc, "Бобік Розкладу");
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-08", "09:00"));

		// Act
		String body = schedule("ADMIN", doctorId, "2026-12-08", "2026-12-09")
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// Assert
		List<String> statuses = JsonPath.read(body, "$.slots[*].status");
		assertThat(statuses.stream().filter("BOOKED"::equals).count(), equalTo(1L));
		assertThat(statuses.get(0), equalTo("BOOKED"));
		assertThat(statuses.get(18), equalTo("FREE"));
	}

	@Test
	@DisplayName("REQ-F-020: усі слоти мають час у форматі ISO 8601 із зсувом і тривалість 30 хвилин")
	void everySlot_hasOffsetTimestampsAndThirtyMinuteDuration() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Розклад Тривалість");

		// Act
		var response = schedule("ADMIN", doctorId, "2026-12-01", "2026-12-01");

		// Assert
		response.andExpect(jsonPath("$.slots[*].startsAt", everyItem(matchesPattern(OFFSET_TIMESTAMP))))
				.andExpect(jsonPath("$.slots[*].endsAt", everyItem(matchesPattern(OFFSET_TIMESTAMP))))
				.andExpect(jsonPath("$.slots[1].startsAt", equalTo(slot("2026-12-01", "09:30"))))
				.andExpect(jsonPath("$.slots[1].endsAt", equalTo(slot("2026-12-01", "10:00"))));
	}
}
