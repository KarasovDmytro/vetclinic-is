package ua.vetclinic.api.owners;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOwnerRequest(@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone,
		@Email @Pattern(regexp = ".+") @Size(max = 254) String email, @Size(min = 1, max = 300) String address) {

	public CreateOwnerRequest {
		fullName = strip(fullName);
		phone = strip(phone);
		email = strip(email);
		address = strip(address);
	}

	private static String strip(String value) {
		return value == null ? null : value.strip();
	}
}
