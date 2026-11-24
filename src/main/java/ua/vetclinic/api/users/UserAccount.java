package ua.vetclinic.api.users;

import java.util.UUID;

/**
 * Обліковий запис для модулів автентифікації та безпеки, зокрема з хешем пароля.
 *
 * @param id ідентифікатор користувача
 * @param email email у нижньому регістрі
 * @param fullName ПІБ
 * @param role роль
 * @param active чи може користувач входити в систему
 * @param passwordHash хеш BCrypt
 */
public record UserAccount(
    UUID id, String email, String fullName, Role role, boolean active, String passwordHash) {}
