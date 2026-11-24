package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Запис на прийом у вигляді для інших модулів (модуль прийомів).
 *
 * @param id ідентифікатор запису
 * @param doctorId лікар, якому призначено запис
 * @param patientId пацієнт запису
 * @param startsAt початок слота; стає датою прийому
 * @param scheduled {@code true}, якщо запис у статусі «Заплановано»
 */
public record AppointmentInfo(
    UUID id, UUID doctorId, UUID patientId, OffsetDateTime startsAt, boolean scheduled) {}
