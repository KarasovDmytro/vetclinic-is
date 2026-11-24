package ua.vetclinic.api.notifications;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.ResourceNotFoundException;

/** Перегляд сповіщень і позначення їх прочитаними. */
@Service
class NotificationService {

  private final NotificationRepository notificationRepository;

  private final AuditLog auditLog;

  NotificationService(NotificationRepository notificationRepository, AuditLog auditLog) {
    this.notificationRepository = notificationRepository;
    this.auditLog = auditLog;
  }

  /**
   * Повертає сторінку сповіщень, новіші спочатку.
   *
   * @param read фільтр за ознакою прочитання або {@code null}
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки
   * @return сторінка сповіщень
   */
  NotificationPage list(Boolean read, int page, int pageSize) {
    Pageable pageable =
        PageRequest.of(
            page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
    Page<NotificationEntity> notifications =
        read == null
            ? notificationRepository.findAll(pageable)
            : notificationRepository.findByRead(read, pageable);
    return new NotificationPage(
        page,
        pageSize,
        notifications.getTotalElements(),
        notifications.getContent().stream().map(NotificationEntity::toResponse).toList());
  }

  /**
   * Позначає сповіщення прочитаним.
   *
   * @param notificationId ідентифікатор сповіщення
   * @return оновлене сповіщення
   * @throws ResourceNotFoundException якщо сповіщення немає
   */
  NotificationResponse markRead(UUID notificationId) {
    NotificationEntity notification =
        notificationRepository
            .findById(notificationId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Notification not found: " + notificationId));
    notification.markRead();
    NotificationResponse marked = notificationRepository.save(notification).toResponse();
    auditLog.success("NOTIFICATION_MARKED_READ", "NOTIFICATION", notificationId);
    return marked;
  }
}
