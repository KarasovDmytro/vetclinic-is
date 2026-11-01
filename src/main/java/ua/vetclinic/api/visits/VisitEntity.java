package ua.vetclinic.api.visits;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import org.hibernate.annotations.BatchSize;

@Entity
@Table(name = "visits")
class VisitEntity {

	@Id
	private UUID id;

	private UUID appointmentId;

	private UUID patientId;

	private UUID doctorId;

	private OffsetDateTime visitDate;

	private String diagnosis;

	private String prescriptions;

	private String notes;

	@ElementCollection
	@CollectionTable(name = "visit_medications", joinColumns = @JoinColumn(name = "visit_id"))
	@OrderColumn(name = "position")
	@BatchSize(size = 100)
	private List<VisitMedicationEntry> medications;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	protected VisitEntity() {
	}

	VisitEntity(UUID id, UUID appointmentId, UUID patientId, UUID doctorId, OffsetDateTime visitDate,
			String diagnosis, String prescriptions, String notes, List<VisitMedicationEntry> medications,
			OffsetDateTime now) {
		this.id = id;
		this.appointmentId = appointmentId;
		this.patientId = patientId;
		this.doctorId = doctorId;
		this.visitDate = visitDate;
		this.diagnosis = diagnosis;
		this.prescriptions = prescriptions;
		this.notes = notes;
		this.medications = medications;
		this.createdAt = now;
		this.updatedAt = now;
	}

	UUID id() {
		return id;
	}

	UUID patientId() {
		return patientId;
	}

	UUID doctorId() {
		return doctorId;
	}

	void apply(UpdateVisitRequest changes, OffsetDateTime now) {
		if (changes.isProvided("diagnosis")) {
			this.diagnosis = changes.diagnosis();
		}
		if (changes.isProvided("prescriptions")) {
			this.prescriptions = changes.prescriptions();
		}
		if (changes.isProvided("notes")) {
			this.notes = changes.notes();
		}
		this.updatedAt = now;
	}

	VisitResponse toResponse() {
		return new VisitResponse(id, appointmentId, patientId, doctorId, visitDate, diagnosis, prescriptions, notes,
				medications.stream().map(entry -> new VisitMedicationResponse(entry.medicationId(),
						entry.medicationName(), entry.quantity())).toList(),
				createdAt, updatedAt);
	}
}
