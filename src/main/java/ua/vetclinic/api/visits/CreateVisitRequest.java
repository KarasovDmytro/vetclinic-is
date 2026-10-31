package ua.vetclinic.api.visits;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.Strings;

public record CreateVisitRequest(@NotBlank @Size(max = 2000) String diagnosis,
		@Size(min = 1, max = 4000) String prescriptions, @Size(min = 1, max = 4000) String notes,
		@Size(max = 50) List<@NotNull @Valid VisitMedicationInput> medications) {

	public CreateVisitRequest {
		diagnosis = Strings.strip(diagnosis);
		prescriptions = Strings.strip(prescriptions);
		notes = Strings.strip(notes);
		medications = medications == null ? List.of() : medications;
	}
}
