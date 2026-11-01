package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;
import ua.vetclinic.api.LogCapture;

/** REQ-NF-006, AD-22, AD-23: зміни облікових записів фіксуються в журналі аудиту без паролів. */
@ApiIntegrationTest
class UserAuditTest {

	private static final String ADMIN_PASSWORD = "Admin-Passw0rd-2026";

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
	@DisplayName("REQ-NF-006: створення користувача — подія USER_CREATED з виконавцем, сутністю, SUCCESS і requestId")
	void createUser_isAudited() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "audit.users.admin1@clinic.example", "Адмін Перший Іванович", "ADMIN",
				ADMIN_PASSWORD);
		String token = loginAndGetToken(mockMvc, "audit.users.admin1@clinic.example", ADMIN_PASSWORD);

		// Act
		var result = mockMvc.perform(post("/api/v1/users")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "audit.users.vet1@clinic.example",
						  "fullName": "Лікар Перший Петрович",
						  "role": "VETERINARIAN",
						  "password": "Vet-Passw0rd-2026"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();
		String createdId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
		String requestId = result.getResponse().getHeader("X-Request-Id");

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=USER_CREATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=USER"),
				containsString("entityId=" + createdId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + requestId))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-23: пароль і його хеш не потрапляють у журнал при створенні користувача")
	void createUser_doesNotLogPasswordOrHash() throws Exception {
		// Arrange
		createUser(mockMvc, "audit.users.admin2@clinic.example", "Адмін Другий Іванович", "ADMIN", ADMIN_PASSWORD);
		String token = loginAndGetToken(mockMvc, "audit.users.admin2@clinic.example", ADMIN_PASSWORD);
		String newUserPassword = "Created-Secret-Passw0rd-2026";

		// Act
		mockMvc.perform(post("/api/v1/users")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "audit.users.vet2@clinic.example",
						  "fullName": "Лікар Другий Петрович",
						  "role": "VETERINARIAN",
						  "password": "%s"
						}
						""".formatted(newUserPassword)))
				.andExpect(status().isCreated());

		// Assert
		assertThat(audit.lines(), hasItem(containsString("event=USER_CREATED")));
		assertThat(audit.lines(), everyItem(not(containsString(newUserPassword))));
		assertThat(audit.lines(), everyItem(not(containsString("$2a$"))));
		assertThat(audit.lines(), everyItem(not(containsString(ADMIN_PASSWORD))));
	}

	@Test
	@DisplayName("REQ-NF-006: створення з уже зайнятим email — USER_CREATED з outcome=FAILURE і errorCode EMAIL_TAKEN")
	void createUserWithTakenEmail_isAuditedAsFailure() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "audit.users.admin3@clinic.example", "Адмін Третій Іванович", "ADMIN",
				ADMIN_PASSWORD);
		String token = loginAndGetToken(mockMvc, "audit.users.admin3@clinic.example", ADMIN_PASSWORD);
		createUser(mockMvc, "audit.users.taken@clinic.example", "Зайнятий Email Петрович", "VETERINARIAN");

		// Act
		mockMvc.perform(post("/api/v1/users")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "audit.users.taken@clinic.example",
						  "fullName": "Дублікат Email Петрович",
						  "role": "VETERINARIAN",
						  "password": "Vet-Passw0rd-2026"
						}
						"""))
				.andExpect(status().isConflict());

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=USER_CREATED"),
				containsString("actorId=" + adminId),
				containsString("outcome=FAILURE"),
				containsString("errorCode=EMAIL_TAKEN"))));
	}

	@Test
	@DisplayName("REQ-NF-006: деактивація користувача — подія USER_DEACTIVATED з виконавцем і сутністю")
	void deactivateUser_isAudited() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "audit.users.admin4@clinic.example", "Адмін Четвертий Іванович", "ADMIN",
				ADMIN_PASSWORD);
		String token = loginAndGetToken(mockMvc, "audit.users.admin4@clinic.example", ADMIN_PASSWORD);
		String targetId = createUser(mockMvc, "audit.users.target@clinic.example", "Ціль Деактивації Петрович",
				"VETERINARIAN");

		// Act
		var result = mockMvc.perform(post("/api/v1/users/" + targetId + "/deactivate")
				.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=USER_DEACTIVATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=USER"),
				containsString("entityId=" + targetId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.4: запит без автентифікації (401) не потрапляє в журнал аудиту")
	void unauthenticatedRequest_isNotAudited() throws Exception {
		// Arrange
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(post("/api/v1/users")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "email": "audit.users.anon@clinic.example",
						  "fullName": "Анонім Без Токена",
						  "role": "VETERINARIAN",
						  "password": "Vet-Passw0rd-2026"
						}
						"""))
				.andExpect(status().isUnauthorized());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}
}
