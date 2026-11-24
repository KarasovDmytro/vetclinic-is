package ua.vetclinic.api.owners;

import java.util.List;

/**
 * Сторінка власників.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість за фільтром
 * @param items власники сторінки
 */
public record OwnerPage(int page, int pageSize, long total, List<OwnerResponse> items) {}
