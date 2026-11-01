package ua.vetclinic.api.users;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAppointments.createDoctor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import ua.vetclinic.api.ApiIntegrationTest;

/** REQ-F-020, REQ-F-021, AD-06.3: довідник активних лікарів для вибору в розкладі (операція listDoctors). */
@ApiIntegrationTest
class ListDoctorsTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("AD-06.3: адміністратор отримує довідник лікарів — 200, масив без пагінації з id та fullName")
	void adminListsDoctors_returnsPlainArray() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Довідник Лікар Перший");

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors").with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", hasItem(doctorId)))
				.andExpect(jsonPath("$[*].fullName", hasItem("Довідник Лікар Перший")));
	}

	@Test
	@DisplayName("AD-06.3: лікар теж бачить довідник лікарів — 200")
	void veterinarianListsDoctors_returns200() throws Exception {
		// Arrange
		String role = "VETERINARIAN";

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors").with(user("vet").roles(role)));

		// Assert
		response.andExpect(status().isOk());
	}

	@Test
	@DisplayName("AD-06.3: елемент довідника містить лише id та fullName — без email, ролі й прапорців")
	void doctorItem_containsOnlyIdAndFullName() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Довідник Лише Поля");

		// Act
		String body = mockMvc.perform(get("/api/v1/doctors").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		// Assert
		List<Map<String, Object>> doctors = JsonPath.read(body, "$[?(@.id == '" + doctorId + "')]");
		assertThat(doctors.size(), equalTo(1));
		assertThat(doctors.get(0).keySet(), equalTo(java.util.Set.of("id", "fullName")));
	}

	@Test
	@DisplayName("AD-06.3: деактивований лікар не потрапляє в довідник")
	void deactivatedDoctor_isNotListed() throws Exception {
		// Arrange
		String doctorId = createDoctor(mockMvc, "Довідник Деактивований");
		mockMvc.perform(post("/api/v1/users/" + doctorId + "/deactivate").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors").with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", not(hasItem(doctorId))));
	}

	@Test
	@DisplayName("AD-06.3: адміністратори не потрапляють у довідник лікарів")
	void administrators_areNotListed() throws Exception {
		// Arrange
		String adminId = createUser(mockMvc, "doctors.admin." + UUID.randomUUID() + "@clinic.example",
				"Довідник Адміністратор", "ADMIN");

		// Act
		var response = mockMvc.perform(get("/api/v1/doctors").with(user("admin").roles("ADMIN")));

		// Assert
		response.andExpect(status().isOk())
				.andExpect(jsonPath("$[*].id", not(hasItem(adminId))));
	}

	@Test
	@DisplayName("AD-06.3: довідник відсортований за fullName за зростанням")
	void doctors_areSortedByFullName() throws Exception {
		// Arrange
		createDoctor(mockMvc, "Сортування Яблуко");
		createDoctor(mockMvc, "Сортування Абрикос");

		// Act
		String body = mockMvc.perform(get("/api/v1/doctors").with(user("admin").roles("ADMIN")))
				.andReturn().getResponse().getContentAsString();

		// Assert
		List<String> names = JsonPath.read(body, "$[*].fullName");
		assertThat(names.indexOf("Сортування Абрикос"), lessThan(names.indexOf("Сортування Яблуко")));
	}

	@Test
	@DisplayName("REQ-NF-002: GET /doctors без токена — 401 UNAUTHORIZED")
	void listDoctorsWithoutToken_returns401() throws Exception {
		// Arrange
		var request = get("/api/v1/doctors");

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code", equalTo("UNAUTHORIZED")));
	}

	@Test
	@DisplayName("AD-09: POST /doctors — 405 METHOD_NOT_ALLOWED, лікарів створюють лише через /users")
	void postDoctors_returns405() throws Exception {
		// Arrange
		var request = post("/api/v1/doctors").with(user("admin").roles("ADMIN"));

		// Act
		var response = mockMvc.perform(request);

		// Assert
		response.andExpect(status().isMethodNotAllowed());
	}
}
