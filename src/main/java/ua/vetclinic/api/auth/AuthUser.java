package ua.vetclinic.api.auth;

import java.util.UUID;
import ua.vetclinic.api.users.Role;

/**
 * Користувач у відповіді на вхід.
 *
 * @param id ідентифікатор користувача
 * @param email email у нижньому регістрі
 * @param fullName ПІБ
 * @param role роль у системі
 */
record AuthUser(UUID id, String email, String fullName, Role role) {}
