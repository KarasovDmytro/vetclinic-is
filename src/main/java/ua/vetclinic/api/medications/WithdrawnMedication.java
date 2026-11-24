package ua.vetclinic.api.medications;

import java.util.UUID;

/**
 * Результат списання препарату під час прийому.
 *
 * @param medicationId медикамент
 * @param name назва на момент списання
 * @param quantity списана кількість
 * @param remainingStock залишок після списання
 * @param minThreshold мінімальний поріг або {@code null}
 */
public record WithdrawnMedication(
    UUID medicationId, String name, int quantity, int remainingStock, Integer minThreshold) {

  /**
   * Перевіряє, чи залишок після списання строго нижчий за поріг (AD-02.2).
   *
   * @return {@code true}, якщо потрібне сповіщення про низький залишок
   */
  public boolean isLow() {
    return minThreshold != null && remainingStock < minThreshold;
  }
}
