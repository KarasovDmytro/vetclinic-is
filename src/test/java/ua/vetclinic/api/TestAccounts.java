package ua.vetclinic.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Спільні кроки Arrange для інтеграційних тестів: створення облікового запису та вхід. */
public final class TestAccounts {

  public static final String DEFAULT_PASSWORD = "Vet-Passw0rd-2026";

  private TestAccounts() {}

  /**
   * Створює обліковий запис з паролем {@link #DEFAULT_PASSWORD} від імені адміністратора.
   *
   * @param mockMvc клієнт тестового застосунку
   * @param email унікальний email
   * @param fullName ПІБ
   * @param role {@code ADMIN} або {@code VETERINARIAN}
   * @return ідентифікатор створеного користувача
   * @throws Exception якщо запит не повернув 201
   */
  public static String createUser(MockMvc mockMvc, String email, String fullName, String role)
      throws Exception {
    return createUser(mockMvc, email, fullName, role, DEFAULT_PASSWORD);
  }

  /**
   * Створює обліковий запис із заданим паролем від імені адміністратора.
   *
   * @param mockMvc клієнт тестового застосунку
   * @param email унікальний email
   * @param fullName ПІБ
   * @param role {@code ADMIN} або {@code VETERINARIAN}
   * @param password пароль від 8 до 72 символів
   * @return ідентифікатор створеного користувача
   * @throws Exception якщо запит не повернув 201
   */
  public static String createUser(
      MockMvc mockMvc, String email, String fullName, String role, String password)
      throws Exception {
    String requestBody =
        """
        {
          "email": "%s",
          "fullName": "%s",
          "role": "%s",
          "password": "%s"
        }
        """
            .formatted(email, fullName, role, password);
    var created =
        mockMvc
            .perform(
                post("/api/v1/users")
                    .with(user("admin").roles("ADMIN"))
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
            .andExpect(status().isCreated())
            .andReturn();
    return JsonPath.read(created.getResponse().getContentAsString(), "$.id");
  }

  /**
   * Виконує вхід і повертає bearer-токен.
   *
   * @param mockMvc клієнт тестового застосунку
   * @param email email користувача
   * @param password пароль користувача
   * @return значення {@code accessToken} з відповіді
   * @throws Exception якщо запит не повернув 200
   */
  public static String loginAndGetToken(MockMvc mockMvc, String email, String password)
      throws Exception {
    String loginBody =
        """
        {
          "email": "%s",
          "password": "%s"
        }
        """
            .formatted(email, password);
    var login =
        mockMvc
            .perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginBody))
            .andExpect(status().isOk())
            .andReturn();
    return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
  }
}
