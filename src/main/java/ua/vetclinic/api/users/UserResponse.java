package ua.vetclinic.api.users;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, Role role, boolean active,
		OffsetDateTime createdAt) {
}
