package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateAppointmentRequest(@NotNull UUID doctorId, @NotNull UUID patientId,
		@NotNull OffsetDateTime startsAt) {
}
