package ua.vetclinic.api.medications;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Медикамент у відповідях API (схема {@code Medication} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор
 * @param name назва
 * @param quantity поточний залишок препарату
 * @param minThreshold мінімальний поріг або {@code null}
 * @param lowStock {@code true}, якщо залишок строго нижчий за поріг
 * @param createdAt час створення
 * @param updatedAt час останньої зміни
 */
public record MedicationResponse(
    UUID id,
    String name,
    int quantity,
    Integer minThreshold,
    boolean lowStock,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
