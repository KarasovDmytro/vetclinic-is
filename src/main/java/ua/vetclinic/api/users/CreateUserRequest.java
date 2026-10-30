package ua.vetclinic.api.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@NotBlank @Email String email, @NotBlank String fullName, @NotNull Role role,
		@NotBlank @Size(min = 8, max = 72) String password) {
}
