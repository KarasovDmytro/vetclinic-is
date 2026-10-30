package ua.vetclinic.api.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-18: єдина модель помилки та заголовки відповідей. */
@ApiIntegrationTest
class ErrorModelTest {

	private static final String UUID_PATTERN =
			"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

	private static final String OFFSET_DATE_TIME_PATTERN =
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*[+-]\\d{2}:\\d{2}";

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-18: відповідь 404 містить code, message, status, timestamp, path і traceId")
	void unknownUser_returns404WithFullErrorModel() throws Exception {
		// Arrange
		String userId = "3f2b8c1e-9d4a-4e7b-8a61-5c0d2e9f7a14";

		// Act
		var response = mockMvc.perform(get("/api/v1/users/" + userId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code", equalTo("NOT_FOUND")))
				.andExpect(jsonPath("$.message", equalTo("Ресурс не знайдено")))
				.andExpect(jsonPath("$.status", equalTo(404)))
				.andExpect(jsonPath("$.timestamp", matchesPattern(OFFSET_DATE_TIME_PATTERN)))
				.andExpect(jsonPath("$.path", equalTo("/api/v1/users/" + userId)))
				.andExpect(jsonPath("$.traceId", matchesPattern(UUID_PATTERN)));
	}

	@Test
	@DisplayName("AD-18: заголовок X-Request-Id збігається з traceId у тілі помилки")
	void errorResponse_hasRequestIdHeaderEqualToTraceId() throws Exception {
		// Arrange
		String userId = "8a5d1f60-2b3c-4d9e-9f47-61e0c3b8a2d5";

		// Act
		var result = mockMvc.perform(get("/api/v1/users/" + userId)
				.with(user("admin").roles("ADMIN")))
				.andReturn();

		// Assert
		String traceId = JsonPath.read(result.getResponse().getContentAsString(), "$.traceId");
		assertThat(result.getResponse().getHeader("X-Request-Id"), equalTo(traceId));
	}

	@Test
	@DisplayName("AD-18: відповідь 401 містить WWW-Authenticate: Bearer і повну модель помилки")
	void anonymousRequest_returns401WithBearerChallengeAndFullErrorModel() throws Exception {
		// Arrange
		String path = "/api/v1/users";

		// Act
		var response = mockMvc.perform(get(path));

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", equalTo("Bearer")))
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")))
				.andExpect(jsonPath("$.message", equalTo("Потрібна автентифікація")))
				.andExpect(jsonPath("$.status", equalTo(401)))
				.andExpect(jsonPath("$.timestamp", matchesPattern(OFFSET_DATE_TIME_PATTERN)))
				.andExpect(jsonPath("$.path", equalTo(path)))
				.andExpect(jsonPath("$.traceId", matchesPattern(UUID_PATTERN)));
	}

	@Test
	@DisplayName("AD-18: відповідь 405 містить заголовок Allow зі списком дозволених методів")
	void unsupportedMethod_returns405WithAllowHeader() throws Exception {
		// Arrange
		String userId = "c4e7a9b2-6f10-4d3a-8b55-0e1f2a3c4d5e";

		// Act
		var response = mockMvc.perform(delete("/api/v1/users/" + userId)
				.with(user("admin").roles("ADMIN"))
				.with(csrf()));

		// Assert
		response.andExpect(status().isMethodNotAllowed())
				.andExpect(header().string("Allow", containsString("GET")));
	}

	@Test
	@DisplayName("AD-18: відповідь 422 перелічує помилки полів у details (field, reason, message)")
	void missingFullName_returns422WithFieldDetails() throws Exception {
		// Arrange
		String requestBody = """
				{
				  "email": "tymoshenko@clinic.example",
				  "role": "VETERINARIAN",
				  "password": "Vet-Passw0rd-2026"
				}
				""";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.code", equalTo("VALIDATION_ERROR")))
				.andExpect(jsonPath("$.details.length()", equalTo(1)))
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")))
				.andExpect(jsonPath("$.details[0].message").isNotEmpty());
	}

	@Test
	@DisplayName("AD-18: зламаний JSON — 400 MALFORMED_REQUEST")
	void malformedJson_returns400MalformedRequest() throws Exception {
		// Arrange
		String requestBody = "{\"email\": \"yarema@clinic.example\", \"fullName\": ";

		// Act
		var response = mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")))
				.andExpect(jsonPath("$.message", equalTo("Запит не вдалося розібрати")))
				.andExpect(jsonPath("$.status", equalTo(400)));
	}

	@Test
	@DisplayName("AD-18: ідентифікатор у шляху не є UUID — 400 MALFORMED_REQUEST")
	void nonUuidPathVariable_returns400MalformedRequest() throws Exception {
		// Arrange
		String userId = "not-a-uuid";

		// Act
		var response = mockMvc.perform(get("/api/v1/users/" + userId)
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")))
				.andExpect(jsonPath("$.path", equalTo("/api/v1/users/" + userId)));
	}
}
