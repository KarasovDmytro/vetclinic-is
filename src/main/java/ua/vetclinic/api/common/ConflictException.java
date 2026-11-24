package ua.vetclinic.api.common;

import java.util.List;

/**
 * Конфлікт зі станом даних (відповідь 409) із стабільним кодом помилки та необов'язковими деталями.
 */
public class ConflictException extends RuntimeException {

  private final String code;

  private final String userMessage;

  private final List<ErrorDetail> details;

  /**
   * Створює виняток без деталей.
   *
   * @param code стабільний код помилки, наприклад {@code SLOT_TAKEN}
   * @param userMessage повідомлення українською для користувача
   */
  public ConflictException(String code, String userMessage) {
    this(code, userMessage, null);
  }

  /**
   * Створює виняток із деталями за окремими полями.
   *
   * @param code стабільний код помилки, наприклад {@code INSUFFICIENT_STOCK}
   * @param userMessage повідомлення українською для користувача
   * @param details деталі за полями або {@code null}
   */
  public ConflictException(String code, String userMessage, List<ErrorDetail> details) {
    super(code + ": " + userMessage);
    this.code = code;
    this.userMessage = userMessage;
    this.details = details;
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

  /**
   * Повертає деталі за полями для поля {@code details} відповіді.
   *
   * @return деталі або {@code null}, якщо їх немає
   */
  public List<ErrorDetail> details() {
    return details;
  }
}
