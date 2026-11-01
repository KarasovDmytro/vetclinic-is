package ua.vetclinic.api.appointments;

import java.util.List;

public record AppointmentPage(int page, int pageSize, long total, List<AppointmentResponse> items) {
}
