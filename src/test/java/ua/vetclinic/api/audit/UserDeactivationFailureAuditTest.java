package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;
import org.springframework.test.web.servlet.ResultActions;

/** AD-22.4: відхилена деактивація користувача (409) фіксується в журналі аудиту з кодом помилки. */
@ApiIntegrationTest
@Transactional
class UserDeactivationFailureAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void startCapture() {
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	private ResultActions deactivate(String userId) throws Exception {
		return mockMvc.perform(post("/api/v1/users/" + userId + "/deactivate").with(user("admin").roles("ADMIN")));
	}

	@Test
	@DisplayName("AD-22.4: повторна деактивація (409) — USER_DEACTIVATED з outcome=FAILURE і errorCode USER_ALREADY_DEACTIVATED")
	void deactivateTwice_isAuditedAsFailure() throws Exception {
		// Arrange
		String doctorId = createUser(mockMvc, "audit.deactivate.twice@clinic.example", "Лікар Повторної Деактивації",
				"VETERINARIAN");
		deactivate(doctorId).andExpect(status().isOk());

		// Act
		deactivate(doctorId).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=USER_DEACTIVATED"),
				containsString("entityType=USER"),
				containsString("entityId=" + doctorId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=USER_ALREADY_DEACTIVATED"))));
	}

	@Test
	@DisplayName("AD-22.4: деактивація останнього адміністратора (409) — USER_DEACTIVATED з outcome=FAILURE і errorCode LAST_ADMIN")
	void deactivateLastAdmin_isAuditedAsFailure() throws Exception {
		// Arrange
		jdbcTemplate.update("UPDATE users SET active = FALSE WHERE role = 'ADMIN'");
		String adminId = createUser(mockMvc, "audit.last.admin@clinic.example", "Останній Адмін Аудиту", "ADMIN");

		// Act
		deactivate(adminId).andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=USER_DEACTIVATED"),
				containsString("entityId=" + adminId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=LAST_ADMIN"))));
	}
}
