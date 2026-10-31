package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AppointmentResponse(UUID id, UUID doctorId, UUID patientId, OffsetDateTime startsAt,
		OffsetDateTime endsAt, AppointmentStatus status, UUID visitId, OffsetDateTime createdAt,
		OffsetDateTime updatedAt) {
}
