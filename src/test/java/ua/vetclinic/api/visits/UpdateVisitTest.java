package ua.vetclinic.api.visits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAppointments.createAppointment;
import static ua.vetclinic.api.TestAppointments.createPatientWithOwner;
import static ua.vetclinic.api.TestAppointments.slot;
import static ua.vetclinic.api.TestMedications.createMedication;
import static ua.vetclinic.api.TestMedications.quantityOf;
import static ua.vetclinic.api.TestMedications.uniqueName;
import static ua.vetclinic.api.TestVisits.Doctor;
import static ua.vetclinic.api.TestVisits.createVisit;
import static ua.vetclinic.api.TestVisits.newDoctor;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-031, REQ-F-032, REQ-F-035, AD-07.5, AD-19.3: редагування результатів прийому (операція updateVisit). */
@ApiIntegrationTest
class UpdateVisitTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Doctor author;

	private String patientId;

	private String medicationId;

	private String visitId;

	@BeforeEach
	void saveVisit() throws Exception {
		author = newDoctor(mockMvc, "Лікар Редагування Прийому");
		patientId = createPatientWithOwner(mockMvc, "Пацієнт Редагування Прийому");
		String appointmentId = createAppointment(mockMvc, author.id(), patientId, slot("2026-11-10", "09:00"));
		medicationId = createMedication(mockMvc, uniqueName("Препарат редагування"), 10, null);
		visitId = createVisit(mockMvc, author, appointmentId, """
				{ "diagnosis": "Початковий діагноз", "prescriptions": "Початкові призначення",
				  "notes": "Початкові нотатки", "medications": [ { "medicationId": "%s", "quantity": 4 } ] }
				""".formatted(medicationId));
	}

	private ResultActions patchVisit(Doctor doctor, String requestBody) throws Exception {
		return mockMvc.perform(patch("/api/v1/visits/" + visitId)
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("REQ-F-032: автор змінює діагноз — 200, новий діагноз, призначення й нотатки без змін")
	void authorChangesDiagnosis_changesOnlyDiagnosis() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Уточнений діагноз\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnosis", equalTo("Уточнений діагноз")))
				.andExpect(jsonPath("$.prescriptions", equalTo("Початкові призначення")))
				.andExpect(jsonPath("$.notes", equalTo("Початкові нотатки")));
	}

	@Test
	@DisplayName("REQ-F-032: автор змінює призначення й нотатки одним запитом")
	void authorChangesPrescriptionsAndNotes() throws Exception {
		// Arrange
		String requestBody = "{ \"prescriptions\": \"Нові призначення\", \"notes\": \"Нові нотатки\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnosis", equalTo("Початковий діагноз")))
				.andExpect(jsonPath("$.prescriptions", equalTo("Нові призначення")))
				.andExpect(jsonPath("$.notes", equalTo("Нові нотатки")));
	}

	@Test
	@DisplayName("AD-19.3: prescriptions і notes null очищають поля")
	void nullPrescriptionsAndNotes_clearFields() throws Exception {
		// Arrange
		String requestBody = "{ \"prescriptions\": null, \"notes\": null }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.prescriptions", nullValue()))
				.andExpect(jsonPath("$.notes", nullValue()));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях нових значень обрізаються")
	void newValues_areTrimmed() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"  Обрізаний  \", \"notes\": \" Нотатка \" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnosis", equalTo("Обрізаний")))
				.andExpect(jsonPath("$.notes", equalTo("Нотатка")));
	}

	@Test
	@DisplayName("AD-19.3: updatedAt оновлюється, createdAt, visitDate і doctorId лишаються")
	void update_refreshesUpdatedAtOnly() throws Exception {
		// Arrange
		String before = mockMvc.perform(get("/api/v1/visits/" + visitId).header("Authorization", author.bearer()))
				.andReturn().getResponse().getContentAsString();

		// Act
		String after = patchVisit(author, "{ \"diagnosis\": \"Новий\" }").andReturn().getResponse()
				.getContentAsString();

		// Assert
		assertThat((String) JsonPath.read(after, "$.createdAt"), equalTo(JsonPath.read(before, "$.createdAt")));
		assertThat((String) JsonPath.read(after, "$.visitDate"), equalTo(JsonPath.read(before, "$.visitDate")));
		assertThat((String) JsonPath.read(after, "$.doctorId"), equalTo(JsonPath.read(before, "$.doctorId")));
		assertThat((String) JsonPath.read(after, "$.updatedAt"),
				not(equalTo((String) JsonPath.read(before, "$.updatedAt"))));
	}

	@Test
	@DisplayName("AD-07.5: редагування не змінює перелік препаратів і не списує повторно")
	void update_keepsMedicationsAndStock() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"Новий діагноз\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.medications", hasSize(1)))
				.andExpect(jsonPath("$.medications[0].medicationId", equalTo(medicationId)))
				.andExpect(jsonPath("$.medications[0].quantity", equalTo(4)));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(6));
	}

	@Test
	@DisplayName("AD-07.5: medications у запиті — 422, поле medications, причина IMMUTABLE_FIELD, залишок не змінюється")
	void medicationsInRequest_returns422ImmutableField() throws Exception {
		// Arrange
		String requestBody = "{ \"medications\": [ { \"medicationId\": \"" + medicationId + "\", \"quantity\": 1 } ] }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("medications")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("IMMUTABLE_FIELD")));
		assertThat(quantityOf(mockMvc, medicationId), equalTo(6));
	}

	@Test
	@DisplayName("AD-19.3: порожнє тіло {} — 422, причина EMPTY_BODY")
	void emptyBody_returns422() throws Exception {
		// Arrange
		String requestBody = "{}";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].reason", equalTo("EMPTY_BODY")));
	}

	@Test
	@DisplayName("AD-19.3: невідоме поле — 422, причина UNKNOWN_FIELD")
	void unknownField_returns422() throws Exception {
		// Arrange
		String requestBody = "{ \"temperature\": 38 }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("temperature")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-14: visitDate не змінюється редагуванням — поле в запиті дає 422 UNKNOWN_FIELD")
	void visitDateInRequest_returns422() throws Exception {
		// Arrange
		String requestBody = "{ \"visitDate\": \"2026-11-11T09:00:00+02:00\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("visitDate")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-17.1: діагноз явно null — 422, поле diagnosis, причина REQUIRED")
	void nullDiagnosis_returns422Required() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": null }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: порожній діагноз — 422, поле diagnosis, причина TOO_SHORT")
	void emptyDiagnosis_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.6: діагноз довший за 2000 символів — 422, поле diagnosis, причина TOO_LONG")
	void tooLongDiagnosis_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"diagnosis\": \"" + "Д".repeat(2001) + "\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("diagnosis")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.1: порожні призначення замість null — 422, поле prescriptions, причина TOO_SHORT")
	void emptyPrescriptions_returns422TooShort() throws Exception {
		// Arrange
		String requestBody = "{ \"prescriptions\": \"\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("prescriptions")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17.6: нотатки довші за 4000 символів — 422, поле notes, причина TOO_LONG")
	void tooLongNotes_returns422TooLong() throws Exception {
		// Arrange
		String requestBody = "{ \"notes\": \"" + "Н".repeat(4001) + "\" }";

		// Act
		var response = patchVisit(author, requestBody);

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("notes")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-07.5: інший лікар не може редагувати чужий прийом — 403 NOT_VISIT_AUTHOR, дані без змін")
	void otherDoctor_returns403() throws Exception {
		// Arrange
		Doctor otherDoctor = newDoctor(mockMvc, "Лікар Редагування Інший");

		// Act
		var response = patchVisit(otherDoctor, "{ \"diagnosis\": \"Чужа правка\" }");

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("NOT_VISIT_AUTHOR")));
		mockMvc.perform(get("/api/v1/visits/" + visitId).header("Authorization", author.bearer()))
				.andExpect(jsonPath("$.diagnosis", equalTo("Початковий діагноз")));
	}

	@Test
	@DisplayName("REQ-F-031: адміністратор не може редагувати прийом — 403 FORBIDDEN")
	void admin_returns403() throws Exception {
		// Arrange
		var request = patch("/api/v1/visits/" + visitId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Правка адміністратора\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
	}

	@Test
	@DisplayName("REQ-F-035: картка пацієнта архівна — 409 PATIENT_ARCHIVED, діагноз не змінюється")
	void archivedPatient_returns409() throws Exception {
		// Arrange
		jdbcTemplate.update("update patients set status = 'ARCHIVED' where id = ?::uuid", patientId);

		// Act
		var response = patchVisit(author, "{ \"diagnosis\": \"Правка архівної картки\" }");

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_ARCHIVED")));
		mockMvc.perform(get("/api/v1/visits/" + visitId).header("Authorization", author.bearer()))
				.andExpect(jsonPath("$.diagnosis", equalTo("Початковий діагноз")));
	}

	@Test
	@DisplayName("AD-18.4: чужий лікар на архівній картці отримує 403 NOT_VISIT_AUTHOR раніше за 409")
	void otherDoctorOnArchivedCard_returns403BeforeConflict() throws Exception {
		// Arrange
		Doctor otherDoctor = newDoctor(mockMvc, "Лікар Редагування Сторонній");
		jdbcTemplate.update("update patients set status = 'ARCHIVED' where id = ?::uuid", patientId);

		// Act
		var response = patchVisit(otherDoctor, "{ \"diagnosis\": \"Правка\" }");

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("NOT_VISIT_AUTHOR")));
	}

	@Test
	@DisplayName("AD-18.2: прийому з таким id немає — 404 NOT_FOUND")
	void unknownVisit_returns404() throws Exception {
		// Arrange
		var request = patch("/api/v1/visits/00000000-0000-4000-8000-000000000b07")
				.header("Authorization", author.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Діагноз\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — некоректне тіло для неіснуючого прийому дає 422, а не 404")
	void invalidBodyForUnknownVisit_returns422() throws Exception {
		// Arrange
		var request = patch("/api/v1/visits/00000000-0000-4000-8000-000000000b08")
				.header("Authorization", author.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{}");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnprocessableContent());
	}

	@Test
	@DisplayName("REQ-NF-002: PATCH /visits/{id} без токена — 401 UNAUTHORIZED")
	void withoutToken_returns401() throws Exception {
		// Arrange
		var request = patch("/api/v1/visits/" + visitId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Діагноз\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("AD-18.2: тіло не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void textBody_returns415() throws Exception {
		// Arrange
		var request = patch("/api/v1/visits/" + visitId)
				.header("Authorization", author.bearer())
				.contentType(MediaType.TEXT_PLAIN)
				.content("diagnosis=Текст");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnsupportedMediaType());
	}

	@Test
	@DisplayName("AD-19.3: PUT /visits/{id} — 405 METHOD_NOT_ALLOWED, змінюється лише частково через PATCH")
	void putVisit_returns405() throws Exception {
		// Arrange
		var request = put("/api/v1/visits/" + visitId)
				.header("Authorization", author.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{ \"diagnosis\": \"Заміна\" }");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed());
	}
}
