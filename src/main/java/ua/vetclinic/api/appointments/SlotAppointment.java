package ua.vetclinic.api.appointments;

import java.util.UUID;

public record SlotAppointment(UUID id, UUID patientId, String patientName, AppointmentStatus status) {
}
