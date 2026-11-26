package ua.vetclinic.api.medications;

import java.util.List;

/**
 * Сторінка каталогу медикаментів.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість позицій за фільтром
 * @param items медикаменти сторінки
 */
public record MedicationPage(int page, int pageSize, long total, List<MedicationResponse> items) {}
