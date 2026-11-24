package ua.vetclinic.api.appointments;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Сітка розкладу лікаря у відповіді {@code getDoctorSchedule}.
 *
 * @param doctorId лікар
 * @param from перший день діапазону
 * @param to останній день діапазону
 * @param slotMinutes тривалість слота, хвилин (30)
 * @param slots слоти діапазону в порядку зростання часу
 */
public record ScheduleResponse(
    UUID doctorId, LocalDate from, LocalDate to, int slotMinutes, List<SlotResponse> slots) {}
