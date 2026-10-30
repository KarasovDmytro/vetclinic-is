package ua.vetclinic.api.logging;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.AuditLogCapture;

/** REQ-NF-006, AD-21.5: рядок на кожен HTTP-запит без query-параметрів і тіл. */
@ApiIntegrationTest
class HttpRequestLogTest {

	private final AuditLogCapture httpLog = new AuditLogCapture("HTTP");

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void startCapture() {
		httpLog.start();
	}

	@AfterEach
	void stopCapture() {
		httpLog.stop();
	}

	@Test
	@DisplayName("AD-21.5: рядок містить метод, шлях, код відповіді, тривалість і requestId, що збігається з X-Request-Id")
	void request_isLoggedWithMethodPathStatusDurationAndRequestId() throws Exception {
		// Arrange
		var request = get("/api/v1/users").with(user("admin").roles("ADMIN"));

		// Act
		var result = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

		// Assert
		String requestId = result.getResponse().getHeader("X-Request-Id");
		assertThat(httpLog.lines(), hasItem(allOf(
				containsString("method=GET"),
				containsString("path=/api/v1/users"),
				containsString("status=200"),
				containsString("durationMs="),
				containsString("requestId=" + requestId))));
	}

	@Test
	@DisplayName("AD-21.5: query-параметри (пошуковий рядок з іменами) не потрапляють у журнал")
	void request_isLoggedWithoutQueryParameters() throws Exception {
		// Arrange
		var request = get("/api/v1/users")
				.param("pageSize", "5")
				.param("role", "ADMIN")
				.with(user("admin").roles("ADMIN"));

		// Act
		mockMvc.perform(request).andExpect(status().isOk());

		// Assert
		assertThat(httpLog.lines(), hasItem(containsString("path=/api/v1/users status=200")));
		assertThat(httpLog.lines(), everyItem(not(containsString("pageSize"))));
		assertThat(httpLog.lines(), everyItem(not(containsString("role=ADMIN"))));
	}

	@Test
	@DisplayName("AD-21.5: запит без автентифікації теж логується — status=401")
	void unauthenticatedRequest_isLoggedWithStatus401() throws Exception {
		// Arrange
		var request = get("/api/v1/users");

		// Act
		mockMvc.perform(request).andExpect(status().isUnauthorized());

		// Assert
		assertThat(httpLog.lines(), hasItem(allOf(
				containsString("method=GET"),
				containsString("path=/api/v1/users"),
				containsString("status=401"))));
	}
}
