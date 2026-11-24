package ua.vetclinic.api.appointments;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CreateAppointmentRequest(
    @NotNull UUID doctorId, @NotNull UUID patientId, @NotNull OffsetDateTime startsAt) {}
