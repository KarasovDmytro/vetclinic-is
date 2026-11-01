package ua.vetclinic.api.visits;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestData.CLINIC_OFFSET_TIMESTAMP;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.createVisit;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.visitBodyWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import org.springframework.http.MediaType;

/** REQ-F-030, REQ-F-031, REQ-F-033, REQ-F-034: перегляд прийому (операція getVisit). */
@ApiIntegrationTest
class GetVisitTest {

	@Autowired
	private MockMvc mockMvc;

	private Doctor author;

	private String patientId;

	private String appointmentId;

	private String medicationId;

	private String medicationName;

	private String visitId;

	@BeforeEach
	void saveVisit() throws Exception {
		author = newDoctor(mockMvc, "Лікар Перегляду Прийому");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Перегляду Прийому");
		appointmentId = createAppointment(mockMvc, author.id(), patientId, slot("2026-11-09", "11:00"));
		medicationName = uniqueName("Препарат перегляду");
		medicationId = createMedication(mockMvc, medicationName, 10, null);
		visitId = createVisit(mockMvc, author, appointmentId, visitBodyWith(medicationId, 2));
	}

	@Test
	@DisplayName("REQ-F-033: автор прийому отримує його — 200 з id, appointmentId, patientId, doctorId, visitDate, результатами й препаратами")
	void authorGetsVisit_returnsAllFields() throws Exception {
		// Arrange
		var request = get("/api/v1/visits/" + visitId).header("Authorization", author.bearer());

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(visitId)))
				.andExpect(jsonPath("$.appointmentId", equalTo(appointmentId)))
				.andExpect(jsonPath("$.patientId", equalTo(patientId)))
				.andExpect(jsonPath("$.doctorId", equalTo(author.id())))
				.andExpect(jsonPath("$.visitDate", equalTo(slot("2026-11-09", "11:00"))))
				.andExpect(jsonPath("$.diagnosis", equalTo("Бактеріальна інфекція")))
				.andExpect(jsonPath("$.medications", hasSize(1)))
				.andExpect(jsonPath("$.medications[0].medicationId", equalTo(medicationId)))
				.andExpect(jsonPath("$.medications[0].medicationName", equalTo(medicationName)))
				.andExpect(jsonPath("$.medications[0].quantity", equalTo(2)))
				.andExpect(jsonPath("$.createdAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)))
				.andExpect(jsonPath("$.updatedAt", matchesPattern(CLINIC_OFFSET_TIMESTAMP)));
	}

	@Test
	@DisplayName("REQ-F-030: інший лікар теж бачить прийом — доступ до карток усіх пацієнтів клініки, 200")
	void otherDoctorGetsVisit_returns200() throws Exception {
		// Arrange
		Doctor otherDoctor = newDoctor(mockMvc, "Лікар Перегляду Інший");

		// Act
		var response = mockMvc.perform(get("/api/v1/visits/" + visitId).header("Authorization", otherDoctor.bearer()));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.doctorId", equalTo(author.id())));
	}

	@Test
	@DisplayName("REQ-F-031: адміністратор не має доступу до прийому — 403 FORBIDDEN")
	void admin_returns403() throws Exception {
		// Arrange
		var request = get("/api/v1/visits/" + visitId).with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-F-031: адміністратор отримує 403 і для неіснуючого прийому — наявність запису не розкривається")
	void adminOnUnknownVisit_returns403Too() throws Exception {
		// Arrange
		var request = get("/api/v1/visits/00000000-0000-4000-8000-000000000b05").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-NF-002: GET /visits/{id} без токена — 401 UNAUTHORIZED")
	void withoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/visits/" + visitId);

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-18.2: прийому з таким id немає — 404 NOT_FOUND")
	void unknownVisit_returns404() throws Exception {
		// Arrange
		var request = get("/api/v1/visits/00000000-0000-4000-8000-000000000b06")
				.header("Authorization", author.bearer());

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.2: id прийому не UUID — 400 MALFORMED_REQUEST")
	void malformedVisitId_returns400() throws Exception {
		// Arrange
		var request = get("/api/v1/visits/не-uuid").header("Authorization", author.bearer());

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-07.5: перейменування препарату після прийому не змінює записаний у прийомі перелік")
	void renamedMedication_doesNotRewriteHistory() throws Exception {
		// Arrange
		mockMvc.perform(patch("/api/v1/medications/" + medicationId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"name\": \"" + uniqueName("Нова назва після прийому") + "\" }"))
				.andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(get("/api/v1/visits/" + visitId).header("Authorization", author.bearer()));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.medications[0].medicationName", equalTo(medicationName)));
	}
}
