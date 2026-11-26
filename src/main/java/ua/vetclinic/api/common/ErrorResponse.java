package ua.vetclinic.api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Тіло будь-якої відповіді 4xx і 5xx (схема {@code Error} в {@code openapi.yaml}).
 *
 * @param code стабільний код помилки, наприклад {@code NOT_FOUND}
 * @param message повідомлення українською
 * @param status HTTP-статус
 * @param timestamp час помилки в часовому поясі клініки
 * @param path шлях запиту
 * @param traceId ідентифікатор запиту, що збігається із заголовком {@code X-Request-Id}
 * @param details помилки за полями; не серіалізується, якщо {@code null}
 */
public record ErrorResponse(
    String code,
    String message,
    int status,
    OffsetDateTime timestamp,
    String path,
    String traceId,
    @JsonInclude(JsonInclude.Include.NON_NULL) List<ErrorDetail> details) {

  /**
   * Створює тіло помилки без деталей за полями.
   *
   * @param status HTTP-статус
   * @param code стабільний код помилки
   * @param message повідомлення українською
   * @param request поточний запит (шлях і {@code requestId})
   * @return тіло помилки
   */
  public static ErrorResponse of(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return of(status, code, message, request, null);
  }

  /**
   * Створює тіло помилки з деталями за полями.
   *
   * @param status HTTP-статус
   * @param code стабільний код помилки
   * @param message повідомлення українською
   * @param request поточний запит (шлях і {@code requestId})
   * @param details помилки за полями або {@code null}
   * @return тіло помилки
   */
  public static ErrorResponse of(
      HttpStatus status,
      String code,
      String message,
      HttpServletRequest request,
      List<ErrorDetail> details) {
    return new ErrorResponse(
        code,
        message,
        status.value(),
        OffsetDateTime.now(ClinicZone.ZONE),
        request.getRequestURI(),
        (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE),
        details);
  }

  /**
   * Створює HTTP-відповідь із тілом помилки без деталей.
   *
   * @param status HTTP-статус
   * @param code стабільний код помилки
   * @param message повідомлення українською
   * @param request поточний запит
   * @return відповідь зі статусом {@code status}
   */
  public static ResponseEntity<ErrorResponse> entity(
      HttpStatus status, String code, String message, HttpServletRequest request) {
    return ResponseEntity.status(status).body(of(status, code, message, request));
  }
}
