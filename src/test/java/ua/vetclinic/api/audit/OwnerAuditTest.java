package ua.vetclinic.api.audit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;
import static ua.vetclinic.api.TestOwners.createOwner;

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

/** REQ-NF-006, AD-22, AD-24.2: реєстрація власника фіксується в журналі аудиту без персональних даних. */
@ApiIntegrationTest
class OwnerAuditTest {

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
	@DisplayName("REQ-NF-006: реєстрація власника — подія OWNER_CREATED з виконавцем і сутністю, без ПІБ і телефону")
	void createOwner_isAuditedWithoutPersonalData() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "audit.owners.admin@clinic.example", "Адмін Власників Іванович", "ADMIN",
				"Admin-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "audit.owners.admin@clinic.example", "Admin-Passw0rd-2026");

		// Act
		var result = mockMvc.perform(post("/api/v1/owners")
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "Приватенко Секрет Іванович",
						  "phone": "+380509998877",
						  "email": "private.owner@example.com",
						  "address": "м. Львів, вул. Секретна, 7"
						}
						"""))
				.andExpect(status().isCreated())
				.andReturn();
		String ownerId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=OWNER_CREATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=OWNER"),
				containsString("entityId=" + ownerId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
		assertThat(audit.lines(), everyItem(not(containsString("Приватенко"))));
		assertThat(audit.lines(), everyItem(not(containsString("+380509998877"))));
		assertThat(audit.lines(), everyItem(not(containsString("private.owner@example.com"))));
	}

	@Test
	@DisplayName("REQ-NF-006: редагування власника — подія OWNER_UPDATED з виконавцем і сутністю, без нових персональних даних")
	void updateOwner_isAuditedWithoutPersonalData() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "audit.owners.admin2@clinic.example", "Адмін Правок Іванович", "ADMIN",
				"Admin-Passw0rd-2026");
		String token = loginAndGetToken(mockMvc, "audit.owners.admin2@clinic.example", "Admin-Passw0rd-2026");
		String ownerId = createOwner(mockMvc, "Редагований Власник Петрович", "+380501010101");

		// Act
		var result = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.header("Authorization", "Bearer " + token)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{ "fullName": "Нове Секретне Прізвище", "phone": "+380502020202" }
						"""))
				.andExpect(status().isOk())
				.andReturn();

		// Assert
		assertThat(audit.lines(), hasItem(allOf(
				containsString("event=OWNER_UPDATED"),
				containsString("actorId=" + adminId),
				containsString("actorRole=ADMIN"),
				containsString("entityType=OWNER"),
				containsString("entityId=" + ownerId),
				containsString("outcome=SUCCESS"),
				containsString("requestId=" + result.getResponse().getHeader("X-Request-Id")))));
		assertThat(audit.lines(), everyItem(not(containsString("Секретне"))));
		assertThat(audit.lines(), everyItem(not(containsString("+380502020202"))));
	}

	@Test
	@DisplayName("REQ-NF-006, AD-22.1: читання власника і списку не фіксується в журналі аудиту")
	void readingOwners_isNotAudited() throws Exception {
		// Arrange
		String ownerId = createOwner(mockMvc, "Лише Читання Власник", "+380503030303");
		int linesBefore = audit.lines().size();

		// Act
		mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/owners").param("q", "Лише Читання").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Assert
		assertThat(audit.lines().size(), equalTo(linesBefore));
	}
}
