package ua.vetclinic.api.users;

/**
 * Роль користувача системи (SRS §3): адміністратор або лікар.
 *
 * <p>Значення передається в JWT як claim {@code role} і визначає доступ до ендпоінтів.
 */
public enum Role {
  ADMIN,
  VETERINARIAN
}
