package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Запис на прийом у відповідях API (схема {@code Appointment} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор запису
 * @param doctorId лікар
 * @param patientId пацієнт
 * @param startsAt початок слота в часовому поясі клініки
 * @param endsAt кінець слота (початок + 30 хвилин)
 * @param status статус запису
 * @param visitId прийом, створений за записом; {@code null}, поки запис не завершено
 * @param createdAt час створення
 * @param updatedAt час останньої зміни
 */
public record AppointmentResponse(
    UUID id,
    UUID doctorId,
    UUID patientId,
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    AppointmentStatus status,
    UUID visitId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
