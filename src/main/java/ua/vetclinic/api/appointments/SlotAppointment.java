package ua.vetclinic.api.appointments;

import java.util.UUID;

/**
 * Запис, що займає слот у сітці розкладу.
 *
 * @param id ідентифікатор запису
 * @param patientId пацієнт
 * @param patientName ім'я пацієнта на момент запиту
 * @param status статус запису
 */
public record SlotAppointment(
    UUID id, UUID patientId, String patientName, AppointmentStatus status) {}
