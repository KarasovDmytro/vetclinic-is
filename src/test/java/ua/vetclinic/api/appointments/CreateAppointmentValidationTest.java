package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-020, AD-05.3, AD-17.5, AD-18: правила слота й полів запису (операція createAppointment). */
@ApiIntegrationTest
class CreateAppointmentValidationTest {

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	private String patientId;

	@BeforeEach
	void createDoctorAndPatient() throws Exception {
		doctorId = createDoctor(mockMvc, "Валідація Запису Лікар");
		patientId = createPatientWithOwner(mockMvc, "Валідація Запису Пацієнт");
	}

	private ResultActions postAppointment(String requestBody) throws Exception {
		return mockMvc.perform(post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	private String body(String startsAtJson) {
		return "{ \"doctorId\": \"" + doctorId + "\", \"patientId\": \"" + patientId + "\", \"startsAt\": "
				+ startsAtJson + " }";
	}

	@Test
	@DisplayName("AD-17.5: час початку не вказано — 422, поле startsAt, причина REQUIRED")
	void missingStartsAt_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"doctorId\": \"" + doctorId + "\", \"patientId\": \"" + patientId + "\" }";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: startsAt явно null — 422, поле startsAt, причина REQUIRED")
	void nullStartsAt_returns422Required() throws Exception {
		// Arrange
		String requestBody = body("null");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.5: лікаря не вказано — 422, поле doctorId, причина REQUIRED")
	void missingDoctorId_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"patientId\": \"" + patientId + "\", \"startsAt\": \"" + slot("2026-12-10", "10:00")
				+ "\" }";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.5: пацієнта не вказано — 422, поле patientId, причина REQUIRED")
	void missingPatientId_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"doctorId\": \"" + doctorId + "\", \"startsAt\": \"" + slot("2026-12-10", "10:00")
				+ "\" }";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("patientId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-05.3: хвилини не 0 і не 30 (10:15) — 422, поле startsAt, причина SLOT_NOT_ALIGNED")
	void minutesNotOnSlotBoundary_returns422SlotNotAligned() throws Exception {
		// Arrange
		String requestBody = body("\"" + slot("2026-12-10", "10:15") + "\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("SLOT_NOT_ALIGNED")));
	}

	@Test
	@DisplayName("AD-17.5: секунди не нуль (10:00:30) — 422, поле startsAt, причина SLOT_NOT_ALIGNED")
	void secondsNotZero_returns422SlotNotAligned() throws Exception {
		// Arrange
		String requestBody = body("\"2026-12-10T10:00:30+02:00\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("SLOT_NOT_ALIGNED")));
	}

	@Test
	@DisplayName("AD-05.3: початок до 09:00 (08:30) — 422, поле startsAt, причина OUTSIDE_WORKING_HOURS")
	void beforeWorkingHours_returns422OutsideWorkingHours() throws Exception {
		// Arrange
		String requestBody = body("\"" + slot("2026-12-10", "08:30") + "\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("startsAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUTSIDE_WORKING_HOURS")));
	}

	@Test
	@DisplayName("AD-05.3: початок о 18:00 — кінець робочого дня, не початок слота — 422 OUTSIDE_WORKING_HOURS")
	void atEndOfWorkingDay_returns422OutsideWorkingHours() throws Exception {
		// Arrange
		String requestBody = body("\"" + slot("2026-12-10", "18:00") + "\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUTSIDE_WORKING_HOURS")));
	}

	@Test
	@DisplayName("AD-05.3: опівночі (00:00) — 422 OUTSIDE_WORKING_HOURS")
	void atMidnight_returns422OutsideWorkingHours() throws Exception {
		// Arrange
		String requestBody = body("\"" + slot("2026-12-10", "00:00") + "\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUTSIDE_WORKING_HOURS")));
	}

	@Test
	@DisplayName("AD-05.3: перший (09:00) і останній (17:30) слоти доби допустимі — 201")
	void firstAndLastSlotOfDay_return201() throws Exception {
		// Arrange
		String first = body("\"" + slot("2026-12-11", "09:00") + "\"");
		String last = body("\"" + slot("2026-12-11", "17:30") + "\"");

		// Act
		var firstResponse = postAppointment(first);
		var lastResponse = postAppointment(last);

		// Assert
		firstResponse.andExpect(status().isCreated());
		lastResponse.andExpect(status().isCreated())
				.andExpect(jsonPath("$.endsAt", equalTo(slot("2026-12-11", "18:00"))));
	}

	@Test
	@DisplayName("AD-05.2: робочі години рахуються за часом клініки, а не UTC — 07:00Z це 09:00 у Києві, допустимо")
	void workingHoursAreEvaluatedInClinicZone() throws Exception {
		// Arrange
		String requestBody = body("\"2026-12-11T07:00:00Z\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startsAt", equalTo(slot("2026-12-11", "09:00"))));
	}

	@Test
	@DisplayName("AD-05.2: 08:00Z у липні — 11:00 за літнім часом +03:00, допустимо; зсув береться за датою")
	void daylightSavingOffsetIsAppliedByDate() throws Exception {
		// Arrange
		String requestBody = body("\"2027-07-01T08:00:00Z\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isCreated())
				.andExpect(jsonPath("$.startsAt", equalTo("2027-07-01T11:00:00+03:00")));
	}

	@Test
	@DisplayName("AD-18.2: startsAt лише датою без часу — 400 MALFORMED_REQUEST")
	void dateWithoutTime_returns400() throws Exception {
		// Arrange
		String requestBody = body("\"2026-12-10\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: startsAt без зсуму часового поясу — 400 MALFORMED_REQUEST")
	void timestampWithoutOffset_returns400() throws Exception {
		// Arrange
		String requestBody = body("\"2026-12-10T10:00:00\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: doctorId не UUID — 400 MALFORMED_REQUEST")
	void doctorIdNotUuid_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"doctorId\": \"abc\", \"patientId\": \"" + patientId + "\", \"startsAt\": \""
				+ slot("2026-12-10", "10:00") + "\" }";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — зсув слота (422) пріоритетніший за архівного пацієнта (409)")
	void invalidSlotForArchivedPatient_returns422Before409() throws Exception {
		// Arrange
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
		String requestBody = body("\"" + slot("2026-12-10", "10:15") + "\"");

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("SLOT_NOT_ALIGNED")));
	}

	@Test
	@DisplayName("AD-18: кілька помилок разом — неіснуючий лікар і незбіг слота в одному details")
	void unknownDoctorAndMisalignedSlot_areBothReported() throws Exception {
		// Arrange
		String requestBody = "{ \"doctorId\": \"00000000-0000-4000-8000-000000000612\", \"patientId\": \""
				+ patientId + "\", \"startsAt\": \"" + slot("2026-12-10", "10:15") + "\" }";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details", hasSize(2)));
	}

	@Test
	@DisplayName("AD-18: невідоме поле в тілі — 422, причина UNKNOWN_FIELD")
	void unknownField_returns422() throws Exception {
		// Arrange
		String requestBody = "{ \"doctorId\": \"" + doctorId + "\", \"patientId\": \"" + patientId
				+ "\", \"startsAt\": \"" + slot("2026-12-10", "10:00") + "\", \"room\": 5 }";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("room")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18: зламаний JSON — 400 MALFORMED_REQUEST")
	void malformedJson_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"doctorId\": ";

		// Act
		var response = postAppointment(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: тіло не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void plainTextBody_returns415() throws Exception {
		// Arrange
		var request = post("/api/v1/appointments")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("doctorId=1");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}
}
