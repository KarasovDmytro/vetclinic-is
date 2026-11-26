package ua.vetclinic.api.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Тіло запиту {@code POST /api/v1/users}: обліковий запис адміністратора або лікаря.
 *
 * @param email унікальний email до 254 символів; зберігається в нижньому регістрі
 * @param fullName ПІБ, до 150 символів
 * @param role роль у системі
 * @param password пароль від 8 до 72 символів; зберігається лише хеш BCrypt
 */
public record CreateUserRequest(
    @NotBlank @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 150) String fullName,
    @NotNull Role role,
    @NotBlank @Size(min = 8, max = 72) String password) {}
