package ua.vetclinic.api.notifications;

import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController {

	private final NotificationService notificationService;

	NotificationController(NotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@GetMapping
	NotificationPage listNotifications(@RequestParam(required = false) Boolean read,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return notificationService.list(read, page, pageSize);
	}

	@PostMapping("/{notificationId}/mark-read")
	NotificationResponse markNotificationRead(@PathVariable UUID notificationId) {
		return notificationService.markRead(notificationId);
	}
}
