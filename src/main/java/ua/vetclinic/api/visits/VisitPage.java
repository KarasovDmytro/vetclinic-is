package ua.vetclinic.api.visits;

import java.util.List;

/**
 * Сторінка хронології прийомів пацієнта.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість прийомів пацієнта
 * @param items прийоми сторінки
 */
public record VisitPage(int page, int pageSize, long total, List<VisitResponse> items) {}
