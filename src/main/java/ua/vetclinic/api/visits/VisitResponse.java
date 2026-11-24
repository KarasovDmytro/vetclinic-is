package ua.vetclinic.api.visits;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Прийом у відповідях API (схема {@code Visit} в {@code openapi.yaml}).
 *
 * @param id ідентифікатор
 * @param appointmentId запис, за яким проведено прийом
 * @param patientId пацієнт
 * @param doctorId лікар, що провів прийом
 * @param visitDate дата прийому (початок слота запису)
 * @param diagnosis діагноз
 * @param prescriptions призначення або {@code null}
 * @param notes нотатки або {@code null}
 * @param medications витрачені препарати
 * @param createdAt час створення
 * @param updatedAt час останньої зміни
 */
public record VisitResponse(
    UUID id,
    UUID appointmentId,
    UUID patientId,
    UUID doctorId,
    OffsetDateTime visitDate,
    String diagnosis,
    String prescriptions,
    String notes,
    List<VisitMedicationResponse> medications,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt) {}
