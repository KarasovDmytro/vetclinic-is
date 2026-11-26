package ua.vetclinic.api.appointments;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Тіло запиту {@code POST /api/v1/appointments}.
 *
 * @param doctorId активний лікар
 * @param patientId пацієнт з активною карткою
 * @param startsAt початок слота: межа 30 хвилин, робочий день і години клініки
 */
public record CreateAppointmentRequest(
    @NotNull UUID doctorId, @NotNull UUID patientId, @NotNull OffsetDateTime startsAt) {}
