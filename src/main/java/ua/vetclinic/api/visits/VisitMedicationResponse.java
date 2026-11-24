package ua.vetclinic.api.visits;

import java.util.UUID;

/**
 * Витрачений препарат у відповіді про прийом.
 *
 * @param medicationId медикамент
 * @param medicationName назва на момент прийому
 * @param quantity списана кількість
 */
public record VisitMedicationResponse(UUID medicationId, String medicationName, int quantity) {}
