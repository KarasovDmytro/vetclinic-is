package ua.vetclinic.api.users;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
class UserEntity {

	@Id
	private UUID id;

	private String email;

	private String fullName;

	private String passwordHash;

	@Enumerated(EnumType.STRING)
	private Role role;

	private boolean active;

	private OffsetDateTime createdAt;

	protected UserEntity() {
	}

	UserEntity(UUID id, String email, String fullName, String passwordHash, Role role, boolean active,
			OffsetDateTime createdAt) {
		this.id = id;
		this.email = email;
		this.fullName = fullName;
		this.passwordHash = passwordHash;
		this.role = role;
		this.active = active;
		this.createdAt = createdAt;
	}

	boolean isActive() {
		return active;
	}

	void deactivate() {
		this.active = false;
	}

	UserResponse toResponse() {
		return new UserResponse(id, email, fullName, role, active, createdAt);
	}
}
