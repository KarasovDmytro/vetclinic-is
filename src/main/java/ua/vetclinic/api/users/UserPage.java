package ua.vetclinic.api.users;

import java.util.List;

/**
 * Сторінка облікових записів.
 *
 * @param page номер сторінки, починаючи з 1
 * @param pageSize розмір сторінки
 * @param total загальна кількість за фільтром
 * @param items користувачі сторінки
 */
public record UserPage(int page, int pageSize, long total, List<UserResponse> items) {}
