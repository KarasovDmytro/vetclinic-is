package ua.vetclinic.api.users;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.ApiIntegrationTest;

/** AD-01.7: деактивація останнього активного адміністратора заборонена. */
@ApiIntegrationTest
@Transactional
class LastAdminTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("AD-01.7: деактивація останнього активного адміністратора — 409 LAST_ADMIN")
	void deactivatingLastActiveAdmin_returns409LastAdmin() throws Exception {
		// Arrange
		jdbcTemplate.update("UPDATE users SET active = FALSE WHERE role = 'ADMIN'");
		String adminId = createUser(mockMvc, "last.admin@clinic.example", "Останній Адмін Петрович", "ADMIN");

		// Act
		var response = mockMvc.perform(post("/api/v1/users/" + adminId + "/deactivate")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()));

		// Assert
		response.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code", equalTo("LAST_ADMIN")))
				.andExpect(jsonPath("$.message", equalTo("Не можна деактивувати останнього адміністратора")));
	}
}
