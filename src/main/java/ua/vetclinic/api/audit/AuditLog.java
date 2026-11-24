package ua.vetclinic.api.audit;

import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Журнал аудиту (REQ-NF-006, AD-22): одна структурована подія в рядок, іменований логер {@code
 * AUDIT}.
 *
 * <p>Кожна подія містить назву дії, виконавця, сутність, результат {@code SUCCESS} або {@code
 * FAILURE} та {@code requestId} запиту. Персональні дані (ПІБ, телефон, ім'я тварини) у журнал не
 * потрапляють.
 */
@Component
public class AuditLog {

  private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

  /**
   * Фіксує успішну дію з явно вказаним виконавцем (вхід у систему, системні події).
   *
   * @param event назва події, наприклад {@code LOGIN_SUCCESS}
   * @param actorId виконавець або {@code null} для системної події
   * @param actorRole роль виконавця, наприклад {@code ADMIN} чи {@code SYSTEM}
   * @param entityType тип сутності, наприклад {@code USER}
   * @param entityId ідентифікатор сутності
   */
  public void success(
      String event, UUID actorId, String actorRole, String entityType, UUID entityId) {
    AUDIT.info(
        "event={} actorId={} actorRole={} entityType={} entityId={} outcome=SUCCESS requestId={}",
        event,
        actorId,
        actorRole,
        entityType,
        entityId,
        MDC.get("requestId"));
  }

  /**
   * Фіксує успішну дію поточного автентифікованого користувача.
   *
   * @param event назва події, наприклад {@code PATIENT_CREATED}
   * @param entityType тип сутності
   * @param entityId ідентифікатор сутності
   */
  public void success(String event, String entityType, UUID entityId) {
    Jwt actor = currentActor();
    success(event, actorIdOf(actor), actorRoleOf(actor), entityType, entityId);
  }

  /**
   * Фіксує відхилену дію з явно вказаним виконавцем (AD-22.4).
   *
   * @param event назва події, наприклад {@code LOGIN_FAILED}
   * @param actorId виконавець або {@code null}, якщо його не встановлено
   * @param actorRole роль виконавця або {@code null}
   * @param entityType тип сутності
   * @param entityId ідентифікатор сутності або {@code null}, якщо її не створено
   * @param errorCode код помилки відповіді, наприклад {@code INVALID_CREDENTIALS}
   */
  public void failure(
      String event,
      UUID actorId,
      String actorRole,
      String entityType,
      UUID entityId,
      String errorCode) {
    AUDIT.info(
        "event={} actorId={} actorRole={} entityType={} entityId={} outcome=FAILURE requestId={} "
            + "errorCode={}",
        event,
        actorId,
        actorRole,
        entityType,
        entityId,
        MDC.get("requestId"),
        errorCode);
  }

  /**
   * Фіксує відхилену дію поточного автентифікованого користувача (AD-22.4).
   *
   * @param event назва події
   * @param entityType тип сутності
   * @param entityId ідентифікатор сутності або {@code null}, якщо її не створено
   * @param errorCode код помилки відповіді
   */
  public void failure(String event, String entityType, UUID entityId, String errorCode) {
    Jwt actor = currentActor();
    failure(event, actorIdOf(actor), actorRoleOf(actor), entityType, entityId, errorCode);
  }

  private static Jwt currentActor() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication != null && authentication.getPrincipal() instanceof Jwt jwt ? jwt : null;
  }

  private static UUID actorIdOf(Jwt actor) {
    return actor == null ? null : UUID.fromString(actor.getSubject());
  }

  private static String actorRoleOf(Jwt actor) {
    return actor == null ? null : actor.getClaimAsString("role");
  }
}
