package ua.vetclinic.api.notifications;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "notifications")
class NotificationEntity {

	@Id
	private UUID id;

	private String type;

	private UUID medicationId;

	private String medicationName;

	private int quantity;

	private Integer minThreshold;

	@Column(name = "is_read")
	private boolean read;

	private OffsetDateTime createdAt;

	protected NotificationEntity() {
	}

	NotificationEntity(UUID id, UUID medicationId, String medicationName, int quantity, Integer minThreshold,
			OffsetDateTime createdAt) {
		this.id = id;
		this.type = "LOW_STOCK";
		this.medicationId = medicationId;
		this.medicationName = medicationName;
		this.quantity = quantity;
		this.minThreshold = minThreshold;
		this.read = false;
		this.createdAt = createdAt;
	}

	UUID id() {
		return id;
	}

	void markRead() {
		this.read = true;
	}

	NotificationResponse toResponse() {
		return new NotificationResponse(id, type, medicationId, medicationName, quantity, minThreshold, read,
				createdAt);
	}
}
