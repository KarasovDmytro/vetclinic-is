package ua.vetclinic.api.notifications;

import java.util.List;

/**
 * Сторінка сповіщень.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість сповіщень за фільтром
 * @param items сповіщення сторінки
 */
public record NotificationPage(
    int page, int pageSize, long total, List<NotificationResponse> items) {}
