package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

public record UpdateAppointmentRequest(@NotNull OffsetDateTime startsAt) {
}
