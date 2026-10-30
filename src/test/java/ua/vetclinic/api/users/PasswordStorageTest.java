package ua.vetclinic.api.users;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-NF-001: паролі зберігаються лише як bcrypt-хеш (cost не менше 10). */
@ApiIntegrationTest
class PasswordStorageTest {

	private static final String BCRYPT_COST_10_OR_MORE = "\\$2[aby]\\$(1[0-9]|[2-3][0-9])\\$.{53}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("REQ-NF-001: пароль у базі зберігається як bcrypt-хеш із cost не менше 10")
	void createdUser_hasPasswordStoredAsBcryptHash() throws Exception {
		// Arrange
		String password = "Vet-Passw0rd-2026";
		String requestBody = """
				{
				  "email": "hryhorenko@clinic.example",
				  "fullName": "Григоренко Оксана Іванівна",
				  "role": "VETERINARIAN",
				  "password": "%s"
				}
				""".formatted(password);

		// Act
		mockMvc.perform(post("/api/v1/users")
				.with(user("admin").roles("ADMIN"))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated());

		// Assert
		String storedHash = jdbcTemplate.queryForObject(
				"select password_hash from users where email = ?", String.class, "hryhorenko@clinic.example");
		assertThat(storedHash, matchesPattern(BCRYPT_COST_10_OR_MORE));
		assertThat(storedHash, not(containsString(password)));
	}
}
