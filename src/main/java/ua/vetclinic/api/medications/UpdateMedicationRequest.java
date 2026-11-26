package ua.vetclinic.api.medications;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.Strings;

/**
 * PATCH: сеттери запам'ятовують, які поля прийшли. Поле quantity приймається лише для того, щоб
 * відхилити його як незмінюване (AD-08.3).
 */
public class UpdateMedicationRequest {

  private final Set<String> provided = new HashSet<>();

  private boolean quantityProvided;

  @Size(min = 1, max = 150)
  private String name;

  @Min(0)
  @Max(1_000_000)
  private Integer minThreshold;

  /**
   * Приймає нову назву з JSON; пробіли по краях обрізаються.
   *
   * @param name нова назва або {@code null}
   */
  public void setName(String name) {
    this.name = Strings.strip(name);
    provided.add("name");
  }

  /**
   * Приймає новий поріг з JSON; {@code null} вимикає сповіщення для препарату.
   *
   * @param minThreshold новий поріг або {@code null}
   */
  public void setMinThreshold(Integer minThreshold) {
    this.minThreshold = minThreshold;
    provided.add("minThreshold");
  }

  /**
   * Фіксує спробу змінити залишок через PATCH, щоб відхилити її з причиною {@code IMMUTABLE_FIELD}.
   *
   * @param ignored значення з JSON, не використовується
   */
  public void setQuantity(Object ignored) {
    quantityProvided = true;
  }

  String name() {
    return name;
  }

  Integer minThreshold() {
    return minThreshold;
  }

  boolean isProvided(String field) {
    return provided.contains(field);
  }

  boolean isEmpty() {
    return provided.isEmpty() && !quantityProvided;
  }

  List<ErrorDetail> violations() {
    List<ErrorDetail> details = new ArrayList<>();
    if (quantityProvided) {
      details.add(new ErrorDetail("quantity", "IMMUTABLE_FIELD", "Поле не можна змінити"));
    }
    if (isProvided("name") && name == null) {
      details.add(new ErrorDetail("name", "REQUIRED", "Поле обов'язкове"));
    }
    return details;
  }
}
