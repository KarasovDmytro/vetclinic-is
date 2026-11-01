package ua.vetclinic.api.notifications;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponse(UUID id, String type, UUID medicationId, String medicationName, int quantity,
		Integer minThreshold, boolean read, OffsetDateTime createdAt) {
}
