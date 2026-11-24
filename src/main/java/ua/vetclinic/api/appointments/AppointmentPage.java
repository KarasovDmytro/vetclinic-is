package ua.vetclinic.api.appointments;

import java.util.List;

/**
 * Сторінка записів на прийом у відповіді {@code GET /api/v1/appointments}.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість записів за фільтром
 * @param items записи сторінки
 */
public record AppointmentPage(
    int page, int pageSize, long total, List<AppointmentResponse> items) {}
