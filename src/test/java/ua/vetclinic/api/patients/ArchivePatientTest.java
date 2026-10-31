package ua.vetclinic.api.patients;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestOwners.createOwner;
import static ua.vetclinic.api.TestPatients.createPatient;

import java.time.OffsetDateTime;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-015, REQ-F-016, AD-04, AD-09, AD-16: архівація картки замість видалення (операція archivePatient). */
@ApiIntegrationTest
class ArchivePatientTest {

	private static final String UNKNOWN_ID = "00000000-0000-4000-8000-000000000521";

	@Autowired
	private MockMvc mockMvc;

	private String ownerId;

	private String patientId;

	@BeforeEach
	void createPatientToArchive() throws Exception {
		ownerId = createOwner(mockMvc, "Власник Архіву", "+380501234610");
		patientId = createPatient(mockMvc, ownerId, "Архівний Пацієнт", "Кіт");
	}

	private ResultActions archive(String id) throws Exception {
		return mockMvc.perform(post("/api/v1/patients/" + id + "/archive").with(user("admin").roles("ADMIN")));
	}

	private ResultActions getAs(String role, String id) throws Exception {
		return mockMvc.perform(get("/api/v1/patients/" + id).with(user("someone").roles(role)));
	}

	@Test
	@DisplayName("REQ-F-015: адміністратор архівує картку — 200, status ARCHIVED, решта даних без змін")
	void adminArchivesPatient_returns200WithArchivedStatus() throws Exception {
		// Arrange
		String id = patientId;

		// Act
		var response = archive(id);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.id", equalTo(id)))
				.andExpect(jsonPath("$.status", equalTo("ARCHIVED")))
				.andExpect(jsonPath("$.name", equalTo("Архівний Пацієнт")))
				.andExpect(jsonPath("$.ownerId", equalTo(ownerId)));
	}

	@Test
	@DisplayName("REQ-F-016: після архівації картка зберігається — GET повертає її зі статусом ARCHIVED")
	void archivedPatient_isStillReturnedByGet() throws Exception {
		// Arrange
		archive(patientId).andExpect(status().isOk());

		// Act
		var response = getAs("ADMIN", patientId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("ARCHIVED")))
				.andExpect(jsonPath("$.name", equalTo("Архівний Пацієнт")));
	}

	@Test
	@DisplayName("REQ-F-035: лікар може переглядати архівну картку — 200 зі статусом ARCHIVED")
	void veterinarianViewsArchivedPatient_returns200() throws Exception {
		// Arrange
		archive(patientId).andExpect(status().isOk());

		// Act
		var response = getAs("VETERINARIAN", patientId);

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.status", equalTo("ARCHIVED")));
	}

	@Test
	@DisplayName("REQ-F-015: createdAt не змінюється, updatedAt після архівації збільшується")
	void archive_keepsCreatedAtAndMovesUpdatedAt() throws Exception {
		// Arrange
		String before = getAs("ADMIN", patientId).andReturn().getResponse().getContentAsString();

		// Act
		String after = archive(patientId).andExpect(status().isOk()).andReturn().getResponse()
				.getContentAsString();

		// Assert
		assertThat((String) JsonPath.read(after, "$.createdAt"), equalTo(JsonPath.read(before, "$.createdAt")));
		long updatedBefore = OffsetDateTime.parse((String) JsonPath.read(before, "$.updatedAt")).toInstant()
				.toEpochMilli();
		long updatedAfter = OffsetDateTime.parse((String) JsonPath.read(after, "$.updatedAt")).toInstant()
				.toEpochMilli();
		assertThat(updatedAfter, greaterThan(updatedBefore));
	}

	@Test
	@DisplayName("AD-16.1: повторна архівація — 409 PATIENT_ARCHIVED з повною моделлю помилки")
	void archiveTwice_returns409PatientArchived() throws Exception {
		// Arrange
		archive(patientId).andExpect(status().isOk());

		// Act
		var response = archive(patientId);

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("PATIENT_ARCHIVED")))
				.andExpect(jsonPath("$.message", equalTo("Картку пацієнта архівовано")))
				.andExpect(jsonPath("$.status", equalTo(409)))
				.andExpect(jsonPath("$.path", equalTo("/api/v1/patients/" + patientId + "/archive")));
	}

	@Test
	@DisplayName("AD-04.3: архівація необоротна — повторний GET і спроба повторної архівації не повертають ACTIVE")
	void archive_isIrreversible() throws Exception {
		// Arrange
		archive(patientId).andExpect(status().isOk());
		archive(patientId).andExpect(status().isConflict());

		// Act
		var response = getAs("ADMIN", patientId);

		// Assert
		response.andExpect(jsonPath("$.status", equalTo("ARCHIVED")));
	}

	@Test
	@DisplayName("REQ-F-015: архівація одного пацієнта не торкається інших пацієнтів і власника")
	void archive_doesNotAffectOtherPatientsOrOwner() throws Exception {
		// Arrange
		String otherPatientId = createPatient(mockMvc, ownerId, "Сусід По Власнику", "Собака");

		// Act
		archive(patientId).andExpect(status().isOk());

		// Assert
		getAs("ADMIN", otherPatientId).andExpect(jsonPath("$.status", equalTo("ACTIVE")));
		mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("REQ-F-015: архівація неіснуючого пацієнта — 404 NOT_FOUND")
	void archiveUnknownPatient_returns404() throws Exception {
		// Arrange
		String id = UNKNOWN_ID;

		// Act
		var response = archive(id);

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор пацієнта не UUID — 400 MALFORMED_REQUEST")
	void archiveWithInvalidId_returns400() throws Exception {
		// Arrange
		String id = "not-a-uuid";

		// Act
		var response = archive(id);

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("REQ-F-004: лікар не може архівувати картки — 403 FORBIDDEN, картка лишається ACTIVE")
	void veterinarianArchivesPatient_returns403() throws Exception {
		// Arrange
		var request = post("/api/v1/patients/" + patientId + "/archive").with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code", equalTo("FORBIDDEN")));
		getAs("ADMIN", patientId).andExpect(jsonPath("$.status", equalTo("ACTIVE")));
	}

	@Test
	@DisplayName("AD-18.4: порядок перевірок — лікар на неіснуючому пацієнті отримує 403, а не 404")
	void veterinarianArchivesUnknownPatient_returns403Before404() throws Exception {
		// Arrange
		var request = post("/api/v1/patients/" + UNKNOWN_ID + "/archive").with(user("vet").roles("VETERINARIAN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("REQ-NF-002: POST /patients/{id}/archive без токена — 401 UNAUTHORIZED")
	void archiveWithoutToken_returns401() throws Exception {
		// Arrange
		var request = post("/api/v1/patients/" + patientId + "/archive");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-09, REQ-F-016: DELETE /patients/{id} — 405 METHOD_NOT_ALLOWED із Allow, картка не видаляється")
	void deletePatient_returns405AndKeepsPatient() throws Exception {
		// Arrange
		var request = delete("/api/v1/patients/" + patientId).with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code", equalTo("METHOD_NOT_ALLOWED")))
				.andExpect(header().string("Allow", containsString("GET")))
				.andExpect(header().string("Allow", containsString("PATCH")));
		getAs("ADMIN", patientId).andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-09: DELETE /patients — 405 METHOD_NOT_ALLOWED")
	void deletePatientCollection_returns405() throws Exception {
		// Arrange
		var request = delete("/api/v1/patients").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.code", equalTo("METHOD_NOT_ALLOWED")));
	}

	@Test
	@DisplayName("AD-19.4: GET /patients/{id}/archive — 405 METHOD_NOT_ALLOWED, архівація лише через POST")
	void getArchiveEndpoint_returns405() throws Exception {
		// Arrange
		var request = get("/api/v1/patients/" + patientId + "/archive").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("POST")));
	}
}
