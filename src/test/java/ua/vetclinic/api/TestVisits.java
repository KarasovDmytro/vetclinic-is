package ua.vetclinic.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ua.vetclinic.api.TestAccounts.createUser;
import static ua.vetclinic.api.TestAccounts.loginAndGetToken;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** Спільні кроки Arrange для тестів прийомів: лікар із токеном і збереження прийому. */
public final class TestVisits {

	public static final String DEFAULT_VISIT_BODY = "{ \"diagnosis\": \"Бактеріальна інфекція\" }";

	private TestVisits() {
	}

	/** Активний лікар, що вже увійшов у систему: id облікового запису та Bearer-токен. */
	public record Doctor(String id, String token) {

		public String bearer() {
			return "Bearer " + token;
		}
	}

	public static Doctor newDoctor(MockMvc mockMvc, String fullName) throws Exception {
		String email = "visit.doctor." + UUID.randomUUID() + "@clinic.example";
		String id = createUser(mockMvc, email, fullName, "VETERINARIAN", "Vet-Passw0rd-2026");
		return new Doctor(id, loginAndGetToken(mockMvc, email, "Vet-Passw0rd-2026"));
	}

	public static ResultActions postVisit(MockMvc mockMvc, Doctor doctor, String appointmentId, String requestBody)
			throws Exception {
		return mockMvc.perform(post("/api/v1/appointments/" + appointmentId + "/visit")
				.header("Authorization", doctor.bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody));
	}

	/** Зберігає прийом із діагнозом за замовчуванням і повертає id прийому. */
	public static String createVisit(MockMvc mockMvc, Doctor doctor, String appointmentId) throws Exception {
		return createVisit(mockMvc, doctor, appointmentId, DEFAULT_VISIT_BODY);
	}

	public static String createVisit(MockMvc mockMvc, Doctor doctor, String appointmentId, String requestBody)
			throws Exception {
		var created = postVisit(mockMvc, doctor, appointmentId, requestBody)
				.andExpect(status().isCreated()).andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	/** Тіло прийому з одним витраченим препаратом. */
	public static String visitBodyWith(String medicationId, int quantity) {
		return "{ \"diagnosis\": \"Бактеріальна інфекція\", \"medications\": [ { \"medicationId\": \"" + medicationId
				+ "\", \"quantity\": " + quantity + " } ] }";
	}
}
