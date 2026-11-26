package ua.vetclinic.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Тіло запиту {@code POST /api/v1/auth/login}.
 *
 * @param email email у форматі адреси, до 254 символів
 * @param password пароль, до 72 символів (межа BCrypt)
 */
record LoginRequest(
    @NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}
