package ua.vetclinic.api.notifications;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.ResourceNotFoundException;

@Service
class NotificationService {

	private final NotificationRepository notificationRepository;

	private final AuditLog auditLog;

	NotificationService(NotificationRepository notificationRepository, AuditLog auditLog) {
		this.notificationRepository = notificationRepository;
		this.auditLog = auditLog;
	}

	NotificationPage list(Boolean read, int page, int pageSize) {
		Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));
		Page<NotificationEntity> notifications = read == null ? notificationRepository.findAll(pageable)
				: notificationRepository.findByRead(read, pageable);
		return new NotificationPage(page, pageSize, notifications.getTotalElements(),
				notifications.getContent().stream().map(NotificationEntity::toResponse).toList());
	}

	NotificationResponse markRead(UUID notificationId) {
		NotificationEntity notification = notificationRepository.findById(notificationId)
				.orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
		notification.markRead();
		NotificationResponse marked = notificationRepository.save(notification).toResponse();
		auditLog.success("NOTIFICATION_MARKED_READ", "NOTIFICATION", notificationId);
		return marked;
	}
}
