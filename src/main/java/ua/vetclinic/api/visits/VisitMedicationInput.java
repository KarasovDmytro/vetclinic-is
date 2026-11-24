package ua.vetclinic.api.visits;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VisitMedicationInput(
    @NotNull UUID medicationId, @NotNull @Min(1) @Max(100_000) Integer quantity) {}
