package ua.vetclinic.api.owners;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerResponse(UUID id, String fullName, String phone, String email, String address,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
