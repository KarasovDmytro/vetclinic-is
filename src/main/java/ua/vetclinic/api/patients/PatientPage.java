package ua.vetclinic.api.patients;

import java.util.List;

/**
 * Сторінка пацієнтів.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість за фільтром
 * @param items пацієнти сторінки
 */
public record PatientPage(int page, int pageSize, long total, List<PatientResponse> items) {}
