package ua.vetclinic.api.auth;

/** Невідомий email або хибний пароль; відповідь 401 {@code INVALID_CREDENTIALS}. */
class InvalidCredentialsException extends RuntimeException {

  InvalidCredentialsException() {
    super("Invalid credentials");
  }
}
