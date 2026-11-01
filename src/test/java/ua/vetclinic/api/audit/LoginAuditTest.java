package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-NF-006, AD-22: кожна спроба входу фіксується в журналі аудиту. */
@ApiIntegrationTest
class LoginAuditTest {

	private final LogCapture audit = new LogCapture();

	@Autowired
	private MockMvc mockMvc;

	@BeforeEach
	void startCapture() {
		audit.start();
	}

	@AfterEach
	void stopCapture() {
		audit.stop();
	}

	@Test
	@DisplayName("REQ-NF-006: успішний вхід — подія LOGIN_SUCCESS з користувачем, роллю та результатом SUCCESS")
	void successfulLogin_isAudited() throws Exception {
		// Arrange
		String userId = createUser(mockMvc, "audit.login.ok@clinic.example", "Аудитенко Ірина Петрівна", "VETERINARIAN",
				"Vet-Passw0rd-2026");

		// Act
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "audit.login.ok@clinic.example", "password": "Vet-Passw0rd-2026" }
						"""))
				.andExpect(status().isOk());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=LOGIN_SUCCESS"),
				containsString("actorId=" + userId),
				containsString("actorRole=VETERINARIAN"),
				containsString("entityType=USER"),
				containsString("entityId=" + userId),
				containsString("outcome=SUCCESS"))));
	}

	@Test
	@DisplayName("REQ-NF-006: невірний пароль наявного користувача — LOGIN_FAILED з actorId і errorCode INVALID_CREDENTIALS")
	void wrongPassword_isAuditedWithActorId() throws Exception {
		// Arrange
		String userId = createUser(mockMvc, "audit.login.wrong@clinic.example", "Помилко Андрій Олегович",
				"VETERINARIAN", "Vet-Passw0rd-2026");

		// Act
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "audit.login.wrong@clinic.example", "password": "Wrong-Passw0rd-2026" }
						"""))
				.andExpect(status().isUnauthorized());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=LOGIN_FAILED"),
				containsString("actorId=" + userId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=INVALID_CREDENTIALS"))));
	}

	@Test
	@DisplayName("REQ-NF-006: невідомий email — LOGIN_FAILED з actorId=null (AD-22.5)")
	void unknownEmail_isAuditedWithNullActor() throws Exception {
		// Arrange
		String unknownEmail = "audit.login.nobody@clinic.example";

		// Act
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "password": "Vet-Passw0rd-2026" }
						""".formatted(unknownEmail)))
				.andExpect(status().isUnauthorized());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=LOGIN_FAILED"),
				containsString("actorId=null"),
				containsString("outcome=FAILURE"),
				containsString("errorCode=INVALID_CREDENTIALS"))));
	}

	@Test
	@DisplayName("REQ-NF-006: вхід деактивованого користувача — LOGIN_FAILED з errorCode ACCOUNT_DEACTIVATED")
	void deactivatedUserLogin_isAudited() throws Exception {
		// Arrange
		String userId = createUser(mockMvc, "audit.login.off@clinic.example", "Вимкненко Роман Ігорович",
				"VETERINARIAN", "Vet-Passw0rd-2026");
		mockMvc.perform(post("/api/v1/users/" + userId + "/deactivate")
				.with(user("admin").roles("ADMIN"))
				.with(csrf()))
				.andExpect(status().isOk());

		// Act
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "audit.login.off@clinic.example", "password": "Vet-Passw0rd-2026" }
						"""))
				.andExpect(status().isForbidden());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=LOGIN_FAILED"),
				containsString("actorId=" + userId),
				containsString("errorCode=ACCOUNT_DEACTIVATED"))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.5: введений email і пароль не потрапляють у журнал при невдалому вході")
	void failedLogin_doesNotLogEmailOrPassword() throws Exception {
		// Arrange
		String typedEmail = "audit.login.secret@clinic.example";
		String typedPassword = "Typed-Secret-Passw0rd-2026";

		// Act
		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "email": "%s", "password": "%s" }
						""".formatted(typedEmail, typedPassword)))
				.andExpect(status().isUnauthorized());

		// Assert
		assertThat(audit.lines(), hasItem(containsString("event=LOGIN_FAILED")));
		assertThat(audit.lines(), everyItem(not(containsString(typedEmail))));
		assertThat(audit.lines(), everyItem(not(containsString(typedPassword))));
	}
}
