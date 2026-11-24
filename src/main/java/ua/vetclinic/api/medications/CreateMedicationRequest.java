package ua.vetclinic.api.medications;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.Strings;

/**
 * Тіло запиту {@code POST /api/v1/medications}: нова позиція каталогу медикаментів.
 *
 * @param name унікальна назва без урахування регістру, до 150 символів
 * @param quantity початковий залишок, від 0 до 1 000 000
 * @param minThreshold мінімальний поріг для сповіщень або {@code null}, якщо сповіщення не потрібні
 */
public record CreateMedicationRequest(
    @NotBlank @Size(max = 150) String name,
    @NotNull @Min(0) @Max(1_000_000) Integer quantity,
    @Min(0) @Max(1_000_000) Integer minThreshold) {

  /** Обрізає пробіли по краях назви (AD-17.1). */
  public CreateMedicationRequest {
    name = Strings.strip(name);
  }
}
