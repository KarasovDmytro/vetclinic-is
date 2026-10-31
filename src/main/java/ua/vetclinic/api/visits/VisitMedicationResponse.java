package ua.vetclinic.api.visits;

import java.util.UUID;

public record VisitMedicationResponse(UUID medicationId, String medicationName, int quantity) {
}
