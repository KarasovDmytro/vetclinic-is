package ua.vetclinic.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Помилка окремого поля запиту в масиві {@code details} відповіді про помилку.
 *
 * @param field шлях до поля, наприклад {@code medications[0].quantity}
 * @param reason стабільна причина, наприклад {@code REQUIRED} або {@code INSUFFICIENT_STOCK}
 * @param message пояснення українською
 * @param params додаткові значення для клієнта; не серіалізується, якщо {@code null}
 */
public record ErrorDetail(
    String field,
    String reason,
    String message,
    @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> params) {

  /**
   * Створює деталь помилки без додаткових значень.
   *
   * @param field шлях до поля
   * @param reason стабільна причина
   * @param message пояснення українською
   */
  public ErrorDetail(String field, String reason, String message) {
    this(field, reason, message, null);
  }
}
