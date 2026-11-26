package ua.vetclinic.api.patients;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;
import ua.vetclinic.api.common.Strings;

/**
 * Тіло запиту {@code POST /api/v1/patients}: реєстрація тварини з прив'язкою до власника.
 *
 * @param ownerId зареєстрований власник
 * @param name кличка, до 100 символів
 * @param species вид тварини, до 50 символів
 * @param breed порода або {@code null}
 * @param sex стать; {@code UNKNOWN}, якщо не передано
 * @param birthDate дата народження не в майбутньому або {@code null}
 */
public record CreatePatientRequest(
    @NotNull UUID ownerId,
    @NotBlank @Size(max = 100) String name,
    @NotBlank @Size(max = 50) String species,
    @Size(min = 1, max = 100) String breed,
    Sex sex,
    LocalDate birthDate) {

  /**
   * Обрізає пробіли в текстових полях і підставляє {@link Sex#UNKNOWN} замість відсутньої статі.
   */
  public CreatePatientRequest {
    name = Strings.strip(name);
    species = Strings.strip(species);
    breed = Strings.strip(breed);
    sex = sex == null ? Sex.UNKNOWN : sex;
  }
}
