package ua.vetclinic.api.common;

/** Недостатньо прав за об'єктом (відповідь 403) із стабільним кодом помилки. */
public class ForbiddenException extends RuntimeException {

  private final String code;

  private final String userMessage;

  /**
   * Створює виняток.
   *
   * @param code стабільний код помилки, наприклад {@code NOT_ASSIGNED_DOCTOR}
   * @param userMessage повідомлення українською для користувача
   */
  public ForbiddenException(String code, String userMessage) {
    super(code + ": " + userMessage);
    this.code = code;
    this.userMessage = userMessage;
  }

  /**
   * Повертає код помилки для поля {@code code} відповіді.
   *
   * @return стабільний код помилки
   */
  public String code() {
    return code;
  }

  /**
   * Повертає повідомлення для поля {@code message} відповіді.
   *
   * @return повідомлення українською
   */
  public String userMessage() {
    return userMessage;
  }
}
