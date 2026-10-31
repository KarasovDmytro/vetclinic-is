package ua.vetclinic.api.appointments;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.cancelAppointment;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createDoctor;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-020, REQ-F-026, AD-19.2, AD-20.3: список записів з фільтрами (операція listAppointments). */
@ApiIntegrationTest
class ListAppointmentsTest {

	@Autowired
	private MockMvc mockMvc;

	private String doctorId;

	private String patientId;

	@BeforeEach
	void createDoctorAndPatient() throws Exception {
		doctorId = createDoctor(mockMvc, "Список Записів Лікар");
		patientId = createPatientWithOwner(mockMvc, "Список Записів Пацієнт");
	}

	private ResultActions list(String role, String... paramsNameValue) throws Exception {
		var request = get("/api/v1/appointments").with(user("someone").roles(role));
		for (int i = 0; i < paramsNameValue.length; i += 2) {
			request.param(paramsNameValue[i], paramsNameValue[i + 1]);
		}
		return mockMvc.perform(request);
	}

	@Test
	@DisplayName("REQ-F-020: адміністратор отримує список записів — 200 з page, pageSize, total, items та полями запису")
	void adminListsAppointments_returns200WithPageAndItems() throws Exception {
		// Arrange
		String appointmentId = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)))
				.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items[0].id", equalTo(appointmentId)))
				.andExpect(jsonPath("$.items[0].doctorId", equalTo(doctorId)))
				.andExpect(jsonPath("$.items[0].patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.items[0].startsAt", equalTo(slot("2026-12-20", "10:00"))))
				.andExpect(jsonPath("$.items[0].endsAt", equalTo(slot("2026-12-20", "10:30"))))
				.andExpect(jsonPath("$.items[0].status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("REQ-F-026: лікар бачить список записів усіх лікарів — 200")
	void veterinarianListsAppointments_returns200() throws Exception {
		// Arrange
		String role = "VETERINARIAN";

		// Act
		var response = list(role);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-19.2: записи відсортовані за startsAt за зростанням")
	void appointments_areSortedByStartsAtAscending() throws Exception {
		// Arrange
		String late = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "16:00"));
		String early = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:00"));
		String middle = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "12:30"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId);

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(early, middle, late)));
	}

	@Test
	@DisplayName("REQ-F-020: фільтр doctorId повертає записи лише цього лікаря")
	void filterByDoctor_returnsOnlyThatDoctorsAppointments() throws Exception {
		// Arrange
		String otherDoctor = createDoctor(mockMvc, "Список Записів Інший Лікар");
		String own = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));
		createAppointment(mockMvc, otherDoctor, patientId, slot("2026-12-20", "10:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId);

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(own)))
				.andExpect(jsonPath("$.items[*].doctorId", everyItem(equalTo(doctorId))));
	}

	@Test
	@DisplayName("REQ-F-020: фільтр patientId повертає записи лише цього пацієнта")
	void filterByPatient_returnsOnlyThatPatientsAppointments() throws Exception {
		// Arrange
		String otherPatient = createPatientWithOwner(mockMvc, "Список Записів Інший Пацієнт");
		String own = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));
		createAppointment(mockMvc, doctorId, otherPatient, slot("2026-12-20", "11:00"));

		// Act
		var response = list("ADMIN", "patientId", patientId);

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(own)));
	}

	@Test
	@DisplayName("REQ-F-025: фільтр status відокремлює скасовані записи від запланованих")
	void filterByStatus_separatesCancelledFromScheduled() throws Exception {
		// Arrange
		String scheduled = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));
		String cancelled = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "11:00"));
		cancelAppointment(mockMvc, cancelled);

		// Act
		var onlyScheduled = list("ADMIN", "doctorId", doctorId, "status", "SCHEDULED");
		var onlyCancelled = list("ADMIN", "doctorId", doctorId, "status", "CANCELLED");

		// Assert
		onlyScheduled.andExpect(jsonPath("$.items[*].id", contains(scheduled)));
		onlyCancelled.andExpect(jsonPath("$.items[*].id", contains(cancelled)));
	}

	@Test
	@DisplayName("REQ-F-025: без фільтра статусу список містить і скасовані записи")
	void defaultList_includesCancelledAppointments() throws Exception {
		// Arrange
		String scheduled = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));
		String cancelled = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "11:00"));
		cancelAppointment(mockMvc, cancelled);

		// Act
		var response = list("ADMIN", "doctorId", doctorId);

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(scheduled, cancelled)));
	}

	@Test
	@DisplayName("AD-20.3: from і to включно в часовому поясі клініки — перший слот першого дня й останній слот останнього дня входять")
	void dateRange_isInclusiveInClinicZone() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-19", "17:30"));
		String firstOfRange = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:00"));
		String lastOfRange = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-21", "17:30"));
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-22", "09:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "from", "2026-12-20", "to", "2026-12-21");

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(firstOfRange, lastOfRange)))
				.andExpect(jsonPath("$.total", equalTo(2)));
	}

	@Test
	@DisplayName("AD-20.3: лише from — записи з цієї дати й пізніше")
	void onlyFrom_returnsAppointmentsFromThatDate() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-19", "10:00"));
		String later = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "from", "2026-12-20");

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(later)));
	}

	@Test
	@DisplayName("AD-20.3: лише to — записи до цієї дати включно")
	void onlyTo_returnsAppointmentsUpToThatDate() throws Exception {
		// Arrange
		String earlier = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-19", "10:00"));
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "to", "2026-12-19");

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(earlier)));
	}

	@Test
	@DisplayName("AD-17.5: для списку записів немає обмеження в 7 днів — діапазон у 30 днів допустимий")
	void listRangeLongerThanWeek_isAllowed() throws Exception {
		// Arrange
		String start = "2026-12-01";
		String end = "2026-12-30";

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "from", start, "to", end);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-17.5: from пізніше за to — 422, поле to, причина INVALID_RANGE")
	void fromAfterTo_returns422InvalidRange() throws Exception {
		// Arrange
		String from = "2026-12-21";
		String to = "2026-12-20";

		// Act
		var response = list("ADMIN", "from", from, "to", to);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("to")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_RANGE")));
	}

	@Test
	@DisplayName("REQ-F-020: фільтри поєднуються — лікар, пацієнт, статус і діапазон дат разом")
	void combinedFilters_applyTogether() throws Exception {
		// Arrange
		String wanted = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-25", "10:00"));
		String otherPatient = createPatientWithOwner(mockMvc, "Список Записів Не Той Пацієнт");
		createAppointment(mockMvc, doctorId, otherPatient, slot("2026-12-20", "11:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "patientId", patientId, "status", "SCHEDULED", "from",
				"2026-12-20", "to", "2026-12-21");

		// Assert
		response.andExpect(jsonPath("$.items[*].id", contains(wanted)));
	}

	@Test
	@DisplayName("AD-15.4: page і pageSize вибирають зріз відсортованого результату, total — загальна кількість")
	void paging_returnsRequestedSliceAndTotal() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:00"));
		String second = createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:30"));
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "10:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "page", "2", "pageSize", "1");

		// Assert
		response.andExpect(jsonPath("$.page", equalTo(2)))
				.andExpect(jsonPath("$.pageSize", equalTo(1)))
				.andExpect(jsonPath("$.total", equalTo(3)))
				.andExpect(jsonPath("$.items[*].id", contains(second)));
	}

	@Test
	@DisplayName("AD-15.4: сторінка за межами результату — 200 з порожнім items і незмінним total")
	void pageBeyondResult_returnsEmptyItems() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:00"));

		// Act
		var response = list("ADMIN", "doctorId", doctorId, "page", "999");

		// Assert
		response.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items", empty()));
	}

	@Test
	@DisplayName("REQ-F-020: записів немає — 200, total 0 і порожній items")
	void noAppointments_returnsEmptyPage() throws Exception {
		// Arrange
		String doctor = doctorId;

		// Act
		var response = list("ADMIN", "doctorId", doctor);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(0)))
				.andExpect(jsonPath("$.items", hasSize(0)));
	}

	@Test
	@DisplayName("AD-17.8: pageSize менший за 1 — 422, поле pageSize, причина OUT_OF_RANGE")
	void zeroPageSize_returns422() throws Exception {
		// Arrange
		String pageSize = "0";

		// Act
		var response = list("ADMIN", "pageSize", pageSize);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("pageSize")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.8: pageSize більший за 100 — 422, поле pageSize, причина OUT_OF_RANGE")
	void tooLargePageSize_returns422() throws Exception {
		// Arrange
		String pageSize = "101";

		// Act
		var response = list("ADMIN", "pageSize", pageSize);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("pageSize")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.8: page менший за 1 — 422, поле page, причина OUT_OF_RANGE")
	void zeroPage_returns422() throws Exception {
		// Arrange
		String page = "0";

		// Act
		var response = list("ADMIN", "page", page);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("page")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-18.2: doctorId у фільтрі не UUID — 400 MALFORMED_REQUEST")
	void doctorIdNotUuid_returns400() throws Exception {
		// Arrange
		String id = "not-a-uuid";

		// Act
		var response = list("ADMIN", "doctorId", id);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: patientId у фільтрі не UUID — 400 MALFORMED_REQUEST")
	void patientIdNotUuid_returns400() throws Exception {
		// Arrange
		String id = "not-a-uuid";

		// Act
		var response = list("ADMIN", "patientId", id);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: невідоме значення status — 400 MALFORMED_REQUEST")
	void invalidStatus_returns400() throws Exception {
		// Arrange
		String statusValue = "DONE";

		// Act
		var response = list("ADMIN", "status", statusValue);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: дата фільтра не у форматі YYYY-MM-DD — 400 MALFORMED_REQUEST")
	void invalidDate_returns400() throws Exception {
		// Arrange
		String from = "20.12.2026";

		// Act
		var response = list("ADMIN", "from", from);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /appointments без токена — 401 UNAUTHORIZED")
	void listWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/appointments");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("REQ-F-020: total у списку без фільтрів не менший за кількість створених записів")
	void listWithoutFilters_hasTotalOfAtLeastCreatedAppointments() throws Exception {
		// Arrange
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:00"));
		createAppointment(mockMvc, doctorId, patientId, slot("2026-12-20", "09:30"));

		// Act
		var response = list("ADMIN");

		// Assert
		response.andExpect(jsonPath("$.total", greaterThanOrEqualTo(2)));
	}
}
