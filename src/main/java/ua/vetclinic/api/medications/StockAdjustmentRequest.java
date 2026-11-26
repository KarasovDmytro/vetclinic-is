package ua.vetclinic.api.medications;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Тіло запиту {@code POST /api/v1/medications/{medicationId}/adjust-stock}.
 *
 * @param delta зміна залишку від -1 000 000 до 1 000 000, не нуль
 */
public record StockAdjustmentRequest(@NotNull @Min(-1_000_000) @Max(1_000_000) Integer delta) {}
