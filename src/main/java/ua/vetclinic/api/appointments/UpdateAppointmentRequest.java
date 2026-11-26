package ua.vetclinic.api.appointments;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

/**
 * Тіло запиту {@code PATCH /api/v1/appointments/{appointmentId}} (перенесення).
 *
 * @param startsAt новий початок слота
 */
public record UpdateAppointmentRequest(@NotNull OffsetDateTime startsAt) {}
