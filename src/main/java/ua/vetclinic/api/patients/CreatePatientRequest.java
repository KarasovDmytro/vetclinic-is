package ua.vetclinic.api.patients;

import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.Strings;

public record CreatePatientRequest(@NotNull UUID ownerId, @NotBlank @Size(max = 100) String name,
		@NotBlank @Size(max = 50) String species, @Size(min = 1, max = 100) String breed, Sex sex,
		LocalDate birthDate) {

	public CreatePatientRequest {
		name = Strings.strip(name);
		species = Strings.strip(species);
		breed = Strings.strip(breed);
		sex = sex == null ? Sex.UNKNOWN : sex;
	}
}
