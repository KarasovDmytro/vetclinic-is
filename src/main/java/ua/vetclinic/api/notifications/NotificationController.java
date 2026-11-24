package ua.vetclinic.api.notifications;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-ресурс сповіщень про низький залишок {@code /api/v1/notifications}; доступний лише
 * адміністратору.
 */
@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {

  private final NotificationService notificationService;

  NotificationController(NotificationService notificationService) {
    this.notificationService = notificationService;
  }

  /**
   * Повертає сторінку сповіщень, новіші спочатку (операція {@code listNotifications}, REQ-F-047).
   *
   * @param read фільтр за ознакою прочитання або {@code null} для всіх
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка сповіщень
   */
  @GetMapping
  NotificationPage listNotifications(
      @RequestParam(required = false) Boolean read,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return notificationService.list(read, page, pageSize);
  }

  /**
   * Позначає сповіщення прочитаним (операція {@code markNotificationRead}, REQ-F-047).
   *
   * @param notificationId ідентифікатор сповіщення
   * @return сповіщення з {@code read = true}
   */
  @PostMapping("/{notificationId}/mark-read")
  NotificationResponse markNotificationRead(@PathVariable UUID notificationId) {
    return notificationService.markRead(notificationId);
  }
}
