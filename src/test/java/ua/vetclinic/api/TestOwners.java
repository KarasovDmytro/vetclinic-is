package ua.vetclinic.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Спільний крок Arrange для інтеграційних тестів: реєстрація власника. */
public final class TestOwners {

	private TestOwners() {
	}

	public static String createOwner(MockMvc mockMvc, String fullName, String phone) throws Exception {
		String requestBody = """
				{
				  "fullName": "%s",
				  "phone": "%s"
				}
				""".formatted(fullName, phone);
		var created = mockMvc.perform(post("/api/v1/owners")
				.with(user("admin").roles("ADMIN"))
				.contentType(MediaType.APPLICATION_JSON)
				.content(requestBody))
				.andExpect(status().isCreated())
				.andReturn();
		return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
	}
}
