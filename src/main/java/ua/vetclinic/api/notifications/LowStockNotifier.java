package ua.vetclinic.api.notifications;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;

/** Створення сповіщень про низький залишок (AD-02) для модуля прийомів. */
@Service
public class LowStockNotifier {

	private final NotificationRepository notificationRepository;

	private final AuditLog auditLog;

	LowStockNotifier(NotificationRepository notificationRepository, AuditLog auditLog) {
		this.notificationRepository = notificationRepository;
		this.auditLog = auditLog;
	}

	/** Створює сповіщення, якщо для препарату ще немає непрочитаного (AD-02.3); викликається в транзакції прийому. */
	public void notifyLowStock(UUID medicationId, String medicationName, int quantity, Integer minThreshold) {
		if (notificationRepository.existsByMedicationIdAndReadFalse(medicationId)) {
			return;
		}
		NotificationEntity notification = notificationRepository.save(new NotificationEntity(UUID.randomUUID(),
				medicationId, medicationName, quantity, minThreshold, OffsetDateTime.now()));
		auditLog.success("LOW_STOCK_NOTIFICATION_CREATED", null, "SYSTEM", "NOTIFICATION", notification.id());
	}
}
