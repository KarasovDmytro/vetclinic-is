package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;

public record SlotResponse(OffsetDateTime startsAt, OffsetDateTime endsAt, SlotStatus status,
		SlotAppointment appointment) {
}
