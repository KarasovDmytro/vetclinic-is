package ua.vetclinic.api.notifications;

import java.util.List;

public record NotificationPage(int page, int pageSize, long total, List<NotificationResponse> items) {
}
