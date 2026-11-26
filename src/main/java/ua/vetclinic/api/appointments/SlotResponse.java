package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;

/**
 * Слот розкладу (SRS §3, схема {@code Slot} в {@code openapi.yaml}).
 *
 * @param startsAt початок слота
 * @param endsAt кінець слота
 * @param status вільний чи зайнятий
 * @param appointment запис, що займає слот; {@code null} для вільного слота
 */
public record SlotResponse(
    OffsetDateTime startsAt,
    OffsetDateTime endsAt,
    SlotStatus status,
    SlotAppointment appointment) {}
