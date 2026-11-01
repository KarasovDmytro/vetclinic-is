package ua.vetclinic.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Спільний крок Arrange для інтеграційних тестів: реєстрація пацієнта. */
public final class TestPatients {

	private TestPatients() {
	}

	public static String createPatient(MockMvc mockMvc, String ownerId, String name, String species) throws Exception {
		String requestBody = """
				{
				  "ownerId": "%s",
				  "name": "%s",
				  "species": "%s"
				}
				""".formatted(ownerId, name, species);
		var created = mockMvc.perform(post("/api/v1/patients")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}
}
