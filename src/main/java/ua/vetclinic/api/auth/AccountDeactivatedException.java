package ua.vetclinic.api.auth;

/**
 * Вхід деактивованого користувача з правильним паролем; відповідь 403 {@code ACCOUNT_DEACTIVATED}
 * (REQ-F-002).
 */
class AccountDeactivatedException extends RuntimeException {

  AccountDeactivatedException() {
    super("Account deactivated");
  }
}
