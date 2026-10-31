package ua.vetclinic.api.visits;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.createVisit;
import static ua.vetclinic.api.TestVisits.newDoctor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-030, REQ-F-031, REQ-F-034, REQ-F-035, AD-14: хронологія прийомів пацієнта (операція listPatientVisits). */
@ApiIntegrationTest
class ListPatientVisitsTest {

	@Autowired
	private MockMvc mockMvc;

	private Doctor doctor;

	private String patientId;

	@BeforeEach
	void createDoctorAndPatient() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Хронології");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Хронології");
	}

	private String visitOn(String patient, String date, String time) throws Exception {
		String appointmentId = createAppointment(mockMvc, doctor.id(), patient, slot(date, time));
		return createVisit(mockMvc, doctor, appointmentId);
	}

	private MockHttpServletRequestBuilder history(String patient) {
		return get("/api/v1/patients/" + patient + "/visits").header("Authorization", doctor.bearer());
	}

	@Test
	@DisplayName("REQ-F-034: хронологія відсортована за visitDate за спаданням, незалежно від порядку збереження")
	void visits_areSortedByVisitDateDescending() throws Exception {
		// Arrange
		String middle = visitOn(patientId, "2026-11-12", "10:00");
		String newest = visitOn(patientId, "2026-11-13", "10:00");
		String oldest = visitOn(patientId, "2026-11-11", "10:00");

		// Act
		var response = mockMvc.perform(history(patientId));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(3)))
				.andExpect(jsonPath("$.items[*].id", contains(newest, middle, oldest)));
	}

	@Test
	@DisplayName("AD-14: сортування за часом слота, а не за часом збереження — прийоми одного дня впорядковані за годиною")
	void sameDayVisits_areOrderedByStartTime() throws Exception {
		// Arrange
		String afternoon = visitOn(patientId, "2026-11-09", "15:00");
		String morning = visitOn(patientId, "2026-11-09", "09:00");

		// Act
		var response = mockMvc.perform(history(patientId));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[*].id", contains(afternoon, morning)));
	}

	@Test
	@DisplayName("REQ-F-034: елемент хронології має ті самі поля, що й прийом, разом із переліком препаратів")
	void item_hasVisitFields() throws Exception {
		// Arrange
		String visitId = visitOn(patientId, "2026-11-10", "10:00");

		// Act
		var response = mockMvc.perform(history(patientId));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id", equalTo(visitId)))
				.andExpect(jsonPath("$.items[0].patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.items[0].doctorId", equalTo(doctor.id())))
				.andExpect(jsonPath("$.items[0].visitDate", equalTo(slot("2026-11-10", "10:00"))))
				.andExpect(jsonPath("$.items[0].diagnosis", equalTo("Бактеріальна інфекція")))
				.andExpect(jsonPath("$.items[0].medications", hasSize(0)));
	}

	@Test
	@DisplayName("REQ-F-034: пацієнт без прийомів — 200, порожній список і total 0")
	void patientWithoutVisits_returnsEmptyPage() throws Exception {
		// Arrange
		var request = history(patientId);

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(0)))
				.andExpect(jsonPath("$.total", equalTo(0)))
				.andExpect(jsonPath("$.page", equalTo(1)))
				.andExpect(jsonPath("$.pageSize", equalTo(20)));
	}

	@Test
	@DisplayName("REQ-F-034: у хронології лише прийоми цього пацієнта")
	void history_containsOnlyThisPatientsVisits() throws Exception {
		// Arrange
		String own = visitOn(patientId, "2026-11-16", "10:00");
		String otherPatient = createPatientWithOwner(mockMvc, "Інший Пацієнт Хронології");
		visitOn(otherPatient, "2026-11-16", "11:00");

		// Act
		var response = mockMvc.perform(history(patientId));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(1)))
				.andExpect(jsonPath("$.items[0].id", equalTo(own)));
	}

	@Test
	@DisplayName("REQ-F-034: хронологія включає прийоми різних лікарів")
	void history_includesVisitsOfAllDoctors() throws Exception {
		// Arrange
		visitOn(patientId, "2026-11-17", "10:00");
		Doctor otherDoctor = newDoctor(mockMvc, "Лікар Хронології Другий");
		String otherAppointment = createAppointment(mockMvc, otherDoctor.id(), patientId, slot("2026-11-18", "10:00"));
		createVisit(mockMvc, otherDoctor, otherAppointment);

		// Act
		var response = mockMvc.perform(history(patientId));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(2)))
				.andExpect(jsonPath("$.items[*].doctorId", contains(otherDoctor.id(), doctor.id())));
	}

	@Test
	@DisplayName("REQ-F-030: лікар бачить хронологію пацієнта, з яким сам не працював — 200")
	void otherDoctor_seesHistory() throws Exception {
		// Arrange
		visitOn(patientId, "2026-11-19", "10:00");
		Doctor stranger = newDoctor(mockMvc, "Лікар Сторонній Хронології");

		// Act
		var response = mockMvc.perform(get("/api/v1/patients/" + patientId + "/visits")
				.header("Authorization", stranger.bearer()));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.total", equalTo(1)));
	}

	@Test
	@DisplayName("REQ-F-035: архівна картка доступна лише для перегляду — хронологія повертається, 200")
	void archivedCard_isStillViewable() throws Exception {
		// Arrange
		String visitId = visitOn(patientId, "2026-11-20", "10:00");
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(history(patientId));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id", equalTo(visitId)));
	}

	@Test
	@DisplayName("REQ-F-031: адміністратор не має доступу до медичної картки — 403 FORBIDDEN")
	void admin_returns403() throws Exception {
		// Arrange
		var request = get("/api/v1/patients/" + patientId + "/visits").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-F-031: адміністратор отримує 403 і для архівної картки")
	void adminOnArchivedCard_returns403() throws Exception {
		// Arrange
		mockMvc.perform(post("/api/v1/patients/" + patientId + "/archive").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(get("/api/v1/patients/" + patientId + "/visits")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-F-031: інші дані пацієнта лишаються доступними адміністратору — GET /patients/{id} дає 200")
	void adminStillReadsPatient() throws Exception {
		// Arrange
		var request = get("/api/v1/patients/" + patientId).with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("REQ-NF-002: GET /patients/{id}/visits без токена — 401 UNAUTHORIZED")
	void withoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/patients/" + patientId + "/visits");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-18.2: пацієнта з таким id немає — 404 NOT_FOUND")
	void unknownPatient_returns404() throws Exception {
		// Arrange
		var request = history("00000000-0000-4000-8000-000000000b09");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.2: id пацієнта не UUID — 400 MALFORMED_REQUEST")
	void malformedPatientId_returns400() throws Exception {
		// Arrange
		var request = history("не-uuid");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-19.2: пагінація — друга сторінка по 2 елементи містить третій і четвертий за спаданням дати")
	void paging_returnsRequestedPage() throws Exception {
		// Arrange
		visitOn(patientId, "2026-11-23", "10:00");
		String fourth = visitOn(patientId, "2026-11-24", "10:00");
		String third = visitOn(patientId, "2026-11-25", "10:00");
		visitOn(patientId, "2026-11-26", "10:00");
		visitOn(patientId, "2026-11-27", "10:00");

		// Act
		var response = mockMvc.perform(history(patientId).param("page", "2").param("pageSize", "2"));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.page", equalTo(2)))
				.andExpect(jsonPath("$.pageSize", equalTo(2)))
				.andExpect(jsonPath("$.total", equalTo(5)))
				.andExpect(jsonPath("$.items[*].id", contains(third, fourth)));
	}

	@Test
	@DisplayName("AD-17.8: pageSize 101 — 422, поле pageSize, причина OUT_OF_RANGE")
	void pageSizeAboveMaximum_returns422() throws Exception {
		// Arrange
		var request = history(patientId).param("pageSize", "101");

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
		var request = history(patientId).param("page", "0");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("page")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}
}
