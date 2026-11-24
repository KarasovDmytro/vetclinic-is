package ua.vetclinic.api.common;

import java.util.List;

/**
 * Помилки полів, виявлені вже в бізнес-логіці (після розбору тіла), відповідь 422 VALIDATION_ERROR.
 */
public class RequestValidationException extends RuntimeException {

  private final List<ErrorDetail> details;

  /**
   * Створює виняток з переліком помилок полів.
   *
   * @param details помилки полів, щонайменше одна
   */
  public RequestValidationException(List<ErrorDetail> details) {
    super("Request fields are invalid: " + details);
    this.details = details;
  }

  /**
   * Повертає помилки полів для поля {@code details} відповіді.
   *
   * @return помилки полів
   */
  public List<ErrorDetail> details() {
    return details;
  }
}
