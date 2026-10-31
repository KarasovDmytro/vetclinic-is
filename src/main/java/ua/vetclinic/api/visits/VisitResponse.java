package ua.vetclinic.api.visits;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record VisitResponse(UUID id, UUID appointmentId, UUID patientId, UUID doctorId, OffsetDateTime visitDate,
		String diagnosis, String prescriptions, String notes, List<VisitMedicationResponse> medications,
		OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
