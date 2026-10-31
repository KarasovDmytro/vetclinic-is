package ua.vetclinic.api.visits;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.quantityOf;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.newDoctor;
import static ua.vetclinic.api.TestVisits.visitBodyWith;

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-07.4, AD-12, AD-17.6, AD-18: правила полів і переліку препаратів прийому (операція createVisit). */
@ApiIntegrationTest
class CreateVisitValidationTest {

	@Autowired
	private MockMvc mockMvc;

	private Doctor doctor;

	private String appointmentId;

	@BeforeEach
	void scheduleAppointment() throws Exception {
		doctor = newDoctor(mockMvc, "Лікар Валідації Прийому");
		String patientId = createPatientWithOwner(mockMvc, "Пацієнт Валідації Прийому");
		appointmentId = createAppointment(mockMvc, doctor.id(), patientId, slot("2026-11-04", "10:00"));
	}

	private ResultActions postVisit(String requestBody) throws Exception {
		return ua.vetclinic.api.TestVisits.postVisit(mockMvc, doctor, appointmentId, requestBody);
	}

	private void assertAppointmentStillScheduled() throws Exception {
		mockMvc.perform(get("/api/v1/appointments/" + appointmentId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.status", equalTo("SCHEDULED")));
	}

	@Test
	@DisplayName("AD-12: діагноз не вказано — 422, поле diagnosis, причина REQUIRED, запис лишається SCHEDULED")
	void missingDiagnosis_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"notes\": \"Лише нотатки\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
		assertAppointmentStillScheduled();
	}

	@Test
	@DisplayName("AD-17.1: діагноз явно null — 422, поле diagnosis, причина REQUIRED")
	void nullDiagnosis_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": null }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-12: порожній діагноз — 422, поле diagnosis, причина REQUIRED")
	void emptyDiagnosis_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: діагноз з самих пробілів — 422, поле diagnosis, причина REQUIRED")
	void blankDiagnosis_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"     \" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.6: діагноз довший за 2000 символів — 422, поле diagnosis, причина TOO_LONG")
	void tooLongDiagnosis_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"" + "Д".repeat(2001) + "\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.6: діагноз рівно з 2000 символів приймається — 201")
	void diagnosisOfMaxLength_returns201() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"" + "Д".repeat(2000) + "\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-17.1: порожні призначення замість null — 422, поле prescriptions, причина TOO_SHORT")
	void emptyPrescriptions_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"prescriptions\": \"\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("prescriptions")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.6: призначення довші за 4000 символів — 422, поле prescriptions, причина TOO_LONG")
	void tooLongPrescriptions_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"prescriptions\": \"" + "П".repeat(4001) + "\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("prescriptions")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.1: порожні нотатки замість null — 422, поле notes, причина TOO_SHORT")
	void emptyNotes_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"notes\": \"\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("notes")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.6: нотатки довші за 4000 символів — 422, поле notes, причина TOO_LONG")
	void tooLongNotes_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"notes\": \"" + "Н".repeat(4001) + "\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("notes")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.6: нотатки рівно з 4000 символів приймаються — 201")
	void notesOfMaxLength_returns201() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"notes\": \"" + "Н".repeat(4000) + "\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-17.6: понад 50 позицій medications — 422, поле medications, причина TOO_LONG")
	void tooManyMedications_returns422TooLong() throws Exception {
		// Arrange
		String items = IntStream.range(0, 51)
				.mapToObj(i -> "{ \"medicationId\": \"" + UUID.randomUUID() + "\", \"quantity\": 1 }")
				.collect(Collectors.joining(", "));
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ " + items + " ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.6: рівно 50 різних препаратів приймаються — 201")
	void fiftyMedications_returns201() throws Exception {
		// Arrange
		String items = IntStream.range(0, 50).mapToObj(i -> {
			try {
				return "{ \"medicationId\": \"" + createMedication(mockMvc, uniqueName("П" + i), 5, null)
						+ "\", \"quantity\": 1 }";
			}
			catch (Exception exception) {
				throw new IllegalStateException(exception);
			}
		}).collect(Collectors.joining(", "));
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ " + items + " ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("AD-17.6: medicationId не вказано — 422, поле medications[0].medicationId, причина REQUIRED")
	void missingMedicationId_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"quantity\": 1 } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].medicationId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.6: кількість не вказано — 422, поле medications[0].quantity, причина REQUIRED")
	void missingQuantity_returns422Required() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Без кількості"), 5, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\" } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-08.1: нульова кількість — 422, поле medications[0].quantity, причина OUT_OF_RANGE (мінімум 1)")
	void zeroQuantity_returns422OutOfRange() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Нуль кількість"), 5, null);
		String requestBody = visitBodyWith(medicationId, 0);

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-08.1: від'ємна кількість — 422, поле medications[0].quantity, причина OUT_OF_RANGE")
	void negativeQuantity_returns422OutOfRange() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Мінус кількість"), 5, null);
		String requestBody = visitBodyWith(medicationId, -2);

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-17.6: кількість понад 100 000 — 422, поле medications[0].quantity, причина OUT_OF_RANGE")
	void quantityAboveMaximum_returns422OutOfRange() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Завелика кількість"), 5, null);
		String requestBody = visitBodyWith(medicationId, 100001);

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].quantity")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("OUT_OF_RANGE")));
	}

	@Test
	@DisplayName("AD-08.1: дробова кількість — 400 MALFORMED_REQUEST, кількість є цілим числом")
	void fractionalQuantity_returns400() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Дробова кількість"), 5, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\", \"quantity\": 1.5 } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: medicationId не UUID — 400 MALFORMED_REQUEST")
	void malformedMedicationId_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"не-uuid\", "
				+ "\"quantity\": 1 } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.3: медикамента немає в каталозі — 422, поле medications[0].medicationId, причина REFERENCE_NOT_FOUND")
	void unknownMedication_returns422ReferenceNotFound() throws Exception {
		// Arrange
		String requestBody = visitBodyWith("00000000-0000-4000-8000-000000000b03", 1);

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[0].medicationId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REFERENCE_NOT_FOUND")));
		assertAppointmentStillScheduled();
	}

	@Test
	@DisplayName("AD-07.4: один препарат двічі — 422, поле другої позиції medications[1].medicationId, причина DUPLICATE_MEDICATION, залишок не змінюється")
	void duplicateMedication_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Дубль у прийомі"), 10, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\", \"quantity\": 1 }, { \"medicationId\": \"" + medicationId + "\", \"quantity\": 2 } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications[1].medicationId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("DUPLICATE_MEDICATION")));
		MatcherAssert.assertThat(quantityOf(mockMvc, medicationId), equalTo(10));
		assertAppointmentStillScheduled();
	}

	@Test
	@DisplayName("AD-18.6: помилки позицій перелічуються разом — неіснуючий препарат і дубль в одному запиті")
	void unknownAndDuplicateMedications_areListedTogether() throws Exception {
		// Arrange
		String known = createMedication(mockMvc, uniqueName("Відомий"), 10, null);
		String unknown = "00000000-0000-4000-8000-000000000b04";
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + unknown
				+ "\", \"quantity\": 1 }, { \"medicationId\": \"" + known + "\", \"quantity\": 1 }, "
				+ "{ \"medicationId\": \"" + known + "\", \"quantity\": 1 } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[*].reason", hasItem("REFERENCE_NOT_FOUND")))
				.andExpect(jsonPath("$.details[*].reason", hasItem("DUPLICATE_MEDICATION")));
	}

	@Test
	@DisplayName("AD-17.1: невідоме поле в позиції препарату — 422, поле name, причина UNKNOWN_FIELD")
	void unknownFieldInMedication_returns422() throws Exception {
		// Arrange
		String medicationId = createMedication(mockMvc, uniqueName("Невідоме поле позиції"), 5, null);
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\", \"quantity\": 1, \"name\": \"Підроблена назва\" } ] }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("name")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: visitDate формує сервер — у запиті 422 UNKNOWN_FIELD")
	void visitDateInRequest_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"visitDate\": \"2026-11-04T10:00:00+02:00\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("visitDate")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-07.1: doctorId береться із запису, а не із запиту — doctorId у запиті 422 UNKNOWN_FIELD")
	void doctorIdInRequest_returns422UnknownField() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"doctorId\": \"" + doctor.id() + "\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("doctorId")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18.2: medications не масив — 400 MALFORMED_REQUEST")
	void medicationsNotArray_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Діагноз\", \"medications\": \"багато\" }";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: зламаний JSON — 400 MALFORMED_REQUEST")
	void brokenJson_returns400() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": ";

		// Act
		var response = postVisit(requestBody);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18.2: тіло запиту відсутнє — 400 MALFORMED_REQUEST")
	void missingBody_returns400() throws Exception {
		// Arrange
		var request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
				.post("/api/v1/appointments/" + appointmentId + "/visit")
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.APPLICATION_JSON);

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}
}
