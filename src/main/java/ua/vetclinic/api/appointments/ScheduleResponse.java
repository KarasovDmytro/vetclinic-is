package ua.vetclinic.api.appointments;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ScheduleResponse(UUID doctorId, LocalDate from, LocalDate to, int slotMinutes,
		List<SlotResponse> slots) {
}
