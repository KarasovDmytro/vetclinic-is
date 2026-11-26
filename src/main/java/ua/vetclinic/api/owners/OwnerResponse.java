package ua.vetclinic.api.owners;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Власник у відповідях API (схема {@code Owner} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор
 * @param fullName ПІБ
 * @param phone телефон
 * @param email email або {@code null}
 * @param address адреса або {@code null}
 * @param createdAt час реєстрації
 * @param updatedAt час останньої зміни
 */
public record OwnerResponse(
    UUID id,
    String fullName,
    String phone,
    String email,
    String address,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
