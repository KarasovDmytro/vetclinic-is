package ua.vetclinic.api.visits;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import ua.vetclinic.api.common.Strings;

/**
 * Тіло запиту {@code POST /api/v1/appointments/{appointmentId}/visit}: результати прийому.
 *
 * @param diagnosis діагноз, до 2000 символів
 * @param prescriptions призначення або {@code null}
 * @param notes нотатки лікаря або {@code null}
 * @param medications витрачені препарати, до 50 позицій без повторів; порожній перелік, якщо не
 *     передано
 */
public record CreateVisitRequest(
    @NotBlank @Size(max = 2000) String diagnosis,
    @Size(min = 1, max = 4000) String prescriptions,
    @Size(min = 1, max = 4000) String notes,
    @Size(max = 50) List<@NotNull @Valid VisitMedicationInput> medications) {

  /** Обрізає пробіли в текстових полях і замінює відсутній перелік препаратів порожнім. */
  public CreateVisitRequest {
    diagnosis = Strings.strip(diagnosis);
    prescriptions = Strings.strip(prescriptions);
    notes = Strings.strip(notes);
    medications = medications == null ? List.of() : medications;
  }
}
