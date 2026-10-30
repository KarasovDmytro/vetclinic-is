package ua.vetclinic.api.owners;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOwnerRequest(@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone, @Email @Size(max = 254) String email,
		@Size(max = 300) String address) {
}
