package ua.vetclinic.api.visits;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VisitMedicationInput(@NotNull UUID medicationId, @NotNull @Min(1) @Max(100_000) Integer quantity) {
}
