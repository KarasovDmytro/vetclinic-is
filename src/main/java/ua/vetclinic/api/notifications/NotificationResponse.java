package ua.vetclinic.api.notifications;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сповіщення у відповідях API (схема {@code Notification} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор
 * @param type тип сповіщення, завжди {@code LOW_STOCK}
 * @param medicationId медикамент
 * @param medicationName назва медикаменту на момент сповіщення
 * @param quantity залишок на момент сповіщення
 * @param minThreshold поріг на момент сповіщення
 * @param read чи сповіщення прочитане
 * @param createdAt час створення
 */
public record NotificationResponse(
    UUID id,
    String type,
    UUID medicationId,
    String medicationName,
    int quantity,
    Integer minThreshold,
    boolean read,
    OffsetDateTime createdAt) {}
