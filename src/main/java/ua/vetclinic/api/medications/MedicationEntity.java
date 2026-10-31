package ua.vetclinic.api.medications;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "medications")
class MedicationEntity {

	@Id
	private UUID id;

	private String name;

	private int quantity;

	private Integer minThreshold;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	protected MedicationEntity() {
	}

	MedicationEntity(UUID id, String name, int quantity, Integer minThreshold, OffsetDateTime now) {
		this.id = id;
		this.name = name;
		this.quantity = quantity;
		this.minThreshold = minThreshold;
		this.createdAt = now;
		this.updatedAt = now;
	}

	int quantity() {
		return quantity;
	}

	void adjust(int delta, OffsetDateTime now) {
		this.quantity += delta;
		this.updatedAt = now;
	}

	void apply(UpdateMedicationRequest changes, OffsetDateTime now) {
		if (changes.isProvided("name")) {
			this.name = changes.name();
		}
		if (changes.isProvided("minThreshold")) {
			this.minThreshold = changes.minThreshold();
		}
		this.updatedAt = now;
	}

	MedicationResponse toResponse() {
		boolean lowStock = minThreshold != null && quantity < minThreshold;
		return new MedicationResponse(id, name, quantity, minThreshold, lowStock, createdAt, updatedAt);
	}
}
