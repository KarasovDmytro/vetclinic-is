package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "appointments")
class AppointmentEntity {

	@Id
	private UUID id;

	private UUID doctorId;

	private UUID patientId;

	private OffsetDateTime startsAt;

	private OffsetDateTime endsAt;

	@Enumerated(EnumType.STRING)
	private AppointmentStatus status;

	private UUID visitId;

	private OffsetDateTime createdAt;

	private OffsetDateTime updatedAt;

	protected AppointmentEntity() {
	}

	AppointmentEntity(UUID id, UUID doctorId, UUID patientId, OffsetDateTime startsAt, OffsetDateTime endsAt,
			AppointmentStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
		this.id = id;
		this.doctorId = doctorId;
		this.patientId = patientId;
		this.startsAt = startsAt;
		this.endsAt = endsAt;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	UUID id() {
		return id;
	}

	UUID doctorId() {
		return doctorId;
	}

	boolean isScheduled() {
		return status == AppointmentStatus.SCHEDULED;
	}

	void cancel(OffsetDateTime now) {
		this.status = AppointmentStatus.CANCELLED;
		this.updatedAt = now;
	}

	void complete(UUID visitId, OffsetDateTime now) {
		this.status = AppointmentStatus.COMPLETED;
		this.visitId = visitId;
		this.updatedAt = now;
	}

	AppointmentInfo toInfo() {
		return new AppointmentInfo(id, doctorId, patientId, startsAt, isScheduled());
	}

	void reschedule(OffsetDateTime newStartsAt, OffsetDateTime newEndsAt, OffsetDateTime now) {
		this.startsAt = newStartsAt;
		this.endsAt = newEndsAt;
		this.updatedAt = now;
	}

	UUID patientId() {
		return patientId;
	}

	OffsetDateTime startsAt() {
		return startsAt;
	}

	AppointmentStatus status() {
		return status;
	}

	AppointmentResponse toResponse() {
		return new AppointmentResponse(id, doctorId, patientId, startsAt, endsAt, status, visitId, createdAt,
				updatedAt);
	}
}
