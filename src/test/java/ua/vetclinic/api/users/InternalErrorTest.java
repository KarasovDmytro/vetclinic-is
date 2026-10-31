package ua.vetclinic.api.users;

import static ua.vetclinic.api.TestData.UUID_PATTERN;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-18: непередбачена помилка не розкриває технічних деталей клієнту. */
@ApiIntegrationTest
class InternalErrorTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private UserService userService;

	@Test
	@DisplayName("AD-18: непередбачена помилка — 500 INTERNAL_ERROR з traceId і без технічних деталей")
	void unexpectedException_returns500InternalErrorWithoutLeakingDetails() throws Exception {
		// Arrange
		String secretDetail = "секретна-внутрішня-деталь";
		when(userService.get(any(UUID.class))).thenThrow(new IllegalStateException(secretDetail));

		// Act
		var response = mockMvc.perform(get("/api/v1/users/5d3c7a1e-8b2f-4c6d-9e40-1a2b3c4d5e6f")
				.with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code", equalTo("INTERNAL_ERROR")))
				.andExpect(jsonPath("$.message", equalTo("Внутрішня помилка сервера")))
				.andExpect(jsonPath("$.status", equalTo(500)))
				.andExpect(jsonPath("$.traceId", matchesPattern(UUID_PATTERN)))
				.andExpect(content().string(not(containsString(secretDetail))));
	}
}
