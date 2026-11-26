package ua.vetclinic.api.patients;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Пацієнт у відповідях API (схема {@code Patient} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор
 * @param ownerId власник
 * @param name кличка
 * @param species вид
 * @param breed порода або {@code null}
 * @param sex стать
 * @param birthDate дата народження або {@code null}
 * @param status статус картки
 * @param createdAt час реєстрації
 * @param updatedAt час останньої зміни
 */
public record PatientResponse(
    UUID id,
    UUID ownerId,
    String name,
    String species,
    String breed,
    Sex sex,
    LocalDate birthDate,
    PatientStatus status,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
