package ua.vetclinic.api.owners;

import static ua.vetclinic.api.TestData.LONG_EMAIL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-011, AD-17, AD-19.3: часткове редагування власника (операція updateOwner). */
@ApiIntegrationTest
class UpdateOwnerValidationTest {

	@Autowired
	private MockMvc mockMvc;

	private String createFullOwner(String fullName) throws Exception {
		var created = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "fullName": "%s",
						  "phone": "+380501234567",
						  "email": "before@example.com",
						  "address": "м. Київ, вул. Старта, 1"
						}
						""".formatted(fullName)))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	private ResultActions patchOwner(String ownerId, String requestBody) throws Exception {
		return mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	@Test
	@DisplayName("REQ-F-011: адміністратор змінює всі поля одразу — 200, нові значення повертаються і через GET")
	void updateAllFields_returns200AndPersists() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Всі Поля Власник");

		// Act
		var response = patchOwner(ownerId, """
				{
				  "fullName": "Нове Повне Імʼя",
				  "phone": "0671112233",
				  "email": "after@example.com",
				  "address": "м. Львів, вул. Нова, 2"
				}
				""");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName", equalTo("Нове Повне Імʼя")))
				.andExpect(jsonPath("$.phone", equalTo("0671112233")))
				.andExpect(jsonPath("$.email", equalTo("after@example.com")))
				.andExpect(jsonPath("$.address", equalTo("м. Львів, вул. Нова, 2")));
		mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName", equalTo("Нове Повне Імʼя")))
				.andExpect(jsonPath("$.address", equalTo("м. Львів, вул. Нова, 2")));
	}

	@Test
	@DisplayName("AD-19.3: відсутні в запиті поля лишаються без змін, зокрема email та адреса")
	void absentFields_remainUnchanged() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Без Змін Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "phone": "0509998877" }
				""");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone", equalTo("0509998877")))
				.andExpect(jsonPath("$.fullName", equalTo("Без Змін Власник")))
				.andExpect(jsonPath("$.email", equalTo("before@example.com")))
				.andExpect(jsonPath("$.address", equalTo("м. Київ, вул. Старта, 1")));
	}

	@Test
	@DisplayName("AD-19.3: address: null очищає адресу")
	void nullAddress_clearsAddress() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Очистка Адреси Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "address": null }
				""");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.address", nullValue()))
				.andExpect(jsonPath("$.email", equalTo("before@example.com")));
		mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andExpect(jsonPath("$.address", nullValue()));
	}

	@Test
	@DisplayName("REQ-F-011: createdAt не змінюється, updatedAt після редагування збільшується")
	void update_keepsCreatedAtAndMovesUpdatedAt() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Часові Мітки Власник");
		String before = mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andReturn().getResponse().getContentAsString();

		// Act
		String after = patchOwner(ownerId, """
				{ "fullName": "Часові Мітки Оновлений" }
				""").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// Assert
		assertThat((String) JsonPath.read(after, "$.createdAt"), equalTo(JsonPath.read(before, "$.createdAt")));
		OffsetDateTime updatedBefore = OffsetDateTime.parse(JsonPath.read(before, "$.updatedAt"));
		OffsetDateTime updatedAfter = OffsetDateTime.parse(JsonPath.read(after, "$.updatedAt"));
		assertThat(updatedAfter.toInstant().toEpochMilli(), greaterThan(updatedBefore.toInstant().toEpochMilli()));
		assertThat((String) JsonPath.read(after, "$.updatedAt"), not(equalTo(JsonPath.read(before, "$.updatedAt"))));
	}

	@Test
	@DisplayName("AD-17.1: пробіли по краях значень при редагуванні обрізаються")
	void surroundingWhitespace_isTrimmed() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Обрізання Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "fullName": "  Обрізаний Після  ", "phone": " 0501112233 ", "address": "  м. Одеса  " }
				""");

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName", equalTo("Обрізаний Після")))
				.andExpect(jsonPath("$.phone", equalTo("0501112233")))
				.andExpect(jsonPath("$.address", equalTo("м. Одеса")));
	}

	@Test
	@DisplayName("AD-17: порожній ПІБ — 422, поле fullName, причина TOO_SHORT")
	void emptyFullName_returns422TooShort() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Порожній ПІБ Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "fullName": "" }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-17: ПІБ довший за 150 символів — 422, поле fullName, причина TOO_LONG")
	void tooLongFullName_returns422TooLong() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Довгий ПІБ Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "fullName": "%s" }
				""".formatted("Ш".repeat(151)));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17.1: fullName: null — обов'язкове поле не буває null, 422 REQUIRED")
	void nullFullName_returns422Required() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Нульовий ПІБ Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "fullName": null }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("fullName")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17.1: phone: null — обов'язкове поле не буває null, 422 REQUIRED")
	void nullPhone_returns422Required() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Нульовий Телефон Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "phone": null }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("phone")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("REQUIRED")));
	}

	@Test
	@DisplayName("AD-17: email без формату адреси — 422, поле email, причина INVALID_FORMAT")
	void invalidEmail_returns422InvalidFormat() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Невірний Email Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "email": "not-an-email" }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("INVALID_FORMAT")));
	}

	@Test
	@DisplayName("AD-17: email довший за 254 символи — 422, поле email, причина TOO_LONG")
	void tooLongEmail_returns422TooLong() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Довгий Email Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "email": "%s" }
				""".formatted(LONG_EMAIL));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("email")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17: адреса довша за 300 символів — 422, поле address, причина TOO_LONG")
	void tooLongAddress_returns422TooLong() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Довга Адреса Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "address": "%s" }
				""".formatted("а".repeat(301)));

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("address")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_LONG")));
	}

	@Test
	@DisplayName("AD-17: порожня адреса замість null — 422, поле address, причина TOO_SHORT")
	void emptyAddress_returns422TooShort() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Порожня Адреса Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "address": "" }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("address")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("TOO_SHORT")));
	}

	@Test
	@DisplayName("AD-19.3: незмінюване поле id у тілі — 422 UNKNOWN_FIELD")
	void immutableIdInBody_returns422() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Зміна Ідентифікатора Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "id": "00000000-0000-4000-8000-000000000001" }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("id")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-19.3: сформовані сервером часові мітки в тілі — 422 UNKNOWN_FIELD")
	void serverManagedTimestampsInBody_return422() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Зміна Міток Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "createdAt": "2020-01-01T00:00:00+02:00" }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("createdAt")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-19.3: невідоме поле в тілі — 422 UNKNOWN_FIELD")
	void unknownField_returns422() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Невідоме Поле Власник");

		// Act
		var response = patchOwner(ownerId, """
				{ "nickname": "Бос" }
				""");

		// Assert
		response.andExpect(status().isUnprocessableContent())
				.andExpect(jsonPath("$.details[0].field", equalTo("nickname")))
				.andExpect(jsonPath("$.details[0].reason", equalTo("UNKNOWN_FIELD")));
	}

	@Test
	@DisplayName("AD-18: PATCH без тіла — 400 MALFORMED_REQUEST")
	void missingBody_returns400() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Без Тіла Власник");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON));

		// Assert
		response.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code", equalTo("MALFORMED_REQUEST")));
	}

	@Test
	@DisplayName("AD-18: PATCH із тілом не application/json — 415 UNSUPPORTED_MEDIA_TYPE")
	void plainTextBody_returns415() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Текстове Тіло Власник");

		// Act
		var response = mockMvc.perform(patch("/api/v1/owners/" + ownerId)
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.TEXT_PLAIN)
				.content("fullName=Інше"));

		// Assert
		response.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.code", equalTo("UNSUPPORTED_MEDIA_TYPE")));
	}

	@Test
	@DisplayName("REQ-F-011: невдале редагування нічого не змінює — дані власника лишаються попередніми")
	void rejectedUpdate_leavesOwnerUnchanged() throws Exception {
		// Arrange
		String ownerId = createFullOwner("Відхилена Правка Власник");

		// Act
		patchOwner(ownerId, """
				{ "fullName": "Не Має Зберегтись", "phone": "12345" }
				""").andExpect(status().isUnprocessableContent());

		// Assert
		mockMvc.perform(get("/api/v1/owners/" + ownerId).with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.fullName", equalTo("Відхилена Правка Власник")))
				.andExpect(jsonPath("$.phone", equalTo("+380501234567")));
	}
}
