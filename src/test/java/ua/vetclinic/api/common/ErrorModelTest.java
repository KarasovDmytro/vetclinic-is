package ua.vetclinic.api.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.TestcontainersConfiguration;

/** AD-18: єдина модель помилки та заголовки відповідей. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
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
}
