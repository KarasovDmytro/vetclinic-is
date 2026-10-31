package ua.vetclinic.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Спільні кроки Arrange для тестів медикаментів, прийомів і сповіщень. */
public final class TestMedications {

	private TestMedications() {
	}

	/** Унікальна назва, щоб тести не конфліктували через спільну БД (назва медикамента унікальна, AD-13). */
	public static String uniqueName(String prefix) {
		return prefix + " " + UUID.randomUUID();
	}

	public static String createMedication(MockMvc mockMvc, String name, int quantity, Integer minThreshold)
			throws Exception {
		String threshold = minThreshold == null ? "" : ", \"minThreshold\": " + minThreshold;
		String requestBody = "{ \"name\": \"" + name + "\", \"quantity\": " + quantity + threshold + " }";
		var created = mockMvc.perform(post("/api/v1/medications")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}

	public static int quantityOf(MockMvc mockMvc, String medicationId) throws Exception {
		var response = mockMvc.perform(get("/api/v1/medications/" + medicationId)
				.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(response.getResponse().getContentAsString(), "$.quantity");
	}
}
