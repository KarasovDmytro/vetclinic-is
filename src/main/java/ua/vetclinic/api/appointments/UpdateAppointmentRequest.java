package ua.vetclinic.api.appointments;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record UpdateAppointmentRequest(@NotNull OffsetDateTime startsAt) {}
