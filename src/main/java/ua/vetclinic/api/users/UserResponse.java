package ua.vetclinic.api.users;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Обліковий запис у відповідях API (схема {@code User} в {@code openapi.yaml}); пароль і його хеш
 * не повертаються.
 *
 * @param id ідентифікатор
 * @param email email у нижньому регістрі
 * @param fullName ПІБ
 * @param role роль
 * @param active чи активний обліковий запис
 * @param createdAt час створення
 */
public record UserResponse(
    UUID id, String email, String fullName, Role role, boolean active, OffsetDateTime createdAt) {}
