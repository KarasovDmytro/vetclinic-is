package ua.vetclinic.api.medications;

import java.time.OffsetDateTime;
import java.util.UUID;

public record MedicationResponse(UUID id, String name, int quantity, Integer minThreshold, boolean lowStock,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
