package ua.vetclinic.api.medications;

import java.util.UUID;

/**
 * Скільки одиниць препарату потрібно списати під час прийому.
 *
 * @param medicationId медикамент каталогу
 * @param quantity кількість до списання, більша за нуль
 */
public record StockWithdrawal(UUID medicationId, int quantity) {}
