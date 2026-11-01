package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Запис для інших модулів: лікар, пацієнт, початок слота та ознака «заплановано». */
public record AppointmentInfo(UUID id, UUID doctorId, UUID patientId, OffsetDateTime startsAt, boolean scheduled) {
}
