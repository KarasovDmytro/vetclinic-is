package ua.vetclinic.api.users;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(@Email String email, String fullName, Role role,
		@Size(min = 8, max = 72) String password) {
}
