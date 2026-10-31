package ua.vetclinic.api.owners;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.Strings;

public record CreateOwnerRequest(@NotBlank @Size(max = 150) String fullName,
		@NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$") String phone,
		@Email @Pattern(regexp = ".+") @Size(max = 254) String email, @Size(min = 1, max = 300) String address) {

	public CreateOwnerRequest {
		fullName = Strings.strip(fullName);
		phone = Strings.strip(phone);
		email = Strings.strip(email);
		address = Strings.strip(address);
	}
}
