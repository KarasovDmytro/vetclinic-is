package ua.vetclinic.api.patients;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patients")
class PatientEntity {

	@Id
	private UUID id;

	private UUID ownerId;

	private String name;

	private String species;

	private String breed;

	@Enumerated(EnumType.STRING)
	private Sex sex;

	private LocalDate birthDate;

	@Enumerated(EnumType.STRING)
	private PatientStatus status;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	protected PatientEntity() {
	}

	PatientEntity(UUID id, UUID ownerId, String name, String species, String breed, Sex sex, LocalDate birthDate,
			PatientStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.id = id;
		this.ownerId = ownerId;
		this.name = name;
		this.species = species;
		this.breed = breed;
		this.sex = sex;
		this.birthDate = birthDate;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	PatientResponse toResponse() {
		return new PatientResponse(id, ownerId, name, species, breed, sex, birthDate, status, createdAt, updatedAt);
	}

	PatientInfo toInfo() {
		return new PatientInfo(id, name, isArchived());
	}

	boolean isArchived() {
		return status == PatientStatus.ARCHIVED;
	}

	void archive(OffsetDateTime now) {
		this.status = PatientStatus.ARCHIVED;
		this.updatedAt = now;
	}

	void apply(UpdatePatientRequest changes, OffsetDateTime now) {
		if (changes.isProvided("name")) {
			this.name = changes.name();
		}
		if (changes.isProvided("species")) {
			this.species = changes.species();
		}
		if (changes.isProvided("breed")) {
			this.breed = changes.breed();
		}
		if (changes.isProvided("sex")) {
			this.sex = changes.sex();
		}
		if (changes.isProvided("birthDate")) {
			this.birthDate = changes.birthDate();
		}
		this.updatedAt = now;
	}
}
