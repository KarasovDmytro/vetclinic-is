package ua.vetclinic.api.owners;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "owners")
class OwnerEntity {

	@Id
	private UUID id;

	private String fullName;

	private String phone;

	private String email;

	private String address;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	protected OwnerEntity() {
	}

	OwnerEntity(UUID id, String fullName, String phone, String email, String address, OffsetDateTime createdAt,
			OffsetDateTime updatedAt) {
		this.id = id;
		this.fullName = fullName;
		this.phone = phone;
		this.email = email;
		this.address = address;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	OwnerResponse toResponse() {
		return new OwnerResponse(id, fullName, phone, email, address, createdAt, updatedAt);
	}

	void apply(UpdateOwnerRequest changes, OffsetDateTime now) {
		if (changes.isProvided("fullName") && changes.fullName() != null) {
			this.fullName = changes.fullName();
		}
		if (changes.isProvided("phone") && changes.phone() != null) {
			this.phone = changes.phone();
		}
		if (changes.isProvided("email")) {
			this.email = changes.email();
		}
		if (changes.isProvided("address")) {
			this.address = changes.address();
		}
		this.updatedAt = now;
	}
}
