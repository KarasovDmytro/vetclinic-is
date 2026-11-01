package ua.vetclinic.api.medications;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.Strings;

public record CreateMedicationRequest(@NotBlank @Size(max = 150) String name,
		@NotNull @Min(0) @Max(1_000_000) Integer quantity, @Min(0) @Max(1_000_000) Integer minThreshold) {

	public CreateMedicationRequest {
		name = Strings.strip(name);
	}
}
