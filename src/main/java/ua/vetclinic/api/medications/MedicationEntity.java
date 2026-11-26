package ua.vetclinic.api.medications;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA-сутність медикаменту каталогу (таблиця {@code medications}).
 *
 * <p>Поле {@code quantity} — залишок препарату (SRS §3); БД не дозволяє йому стати від'ємним.
 */
@Entity
@Table(name = "medications")
class MedicationEntity {

  @Id private UUID id;

  private String name;

  private int quantity;

  private Integer minThreshold;

  private OffsetDateTime createdAt;

  private OffsetDateTime updatedAt;

  protected MedicationEntity() {}

  MedicationEntity(UUID id, String name, int quantity, Integer minThreshold, OffsetDateTime now) {
    this.id = id;
    this.name = name;
    this.quantity = quantity;
    this.minThreshold = minThreshold;
    this.createdAt = now;
    this.updatedAt = now;
  }

  UUID id() {
    return id;
  }

  String name() {
    return name;
  }

  Integer minThreshold() {
    return minThreshold;
  }

  int quantity() {
    return quantity;
  }

  /**
   * Змінює залишок на {@code delta}; межі перевіряє викликач.
   *
   * @param delta зміна залишку, від'ємна для списання
   * @param now час зміни
   */
  void adjust(int delta, OffsetDateTime now) {
    this.quantity += delta;
    this.updatedAt = now;
  }

  /**
   * Застосовує передані в PATCH поля; відсутні поля не змінюються.
   *
   * @param changes поля запиту
   * @param now час зміни
   */
  void apply(UpdateMedicationRequest changes, OffsetDateTime now) {
    if (changes.isProvided("name")) {
      this.name = changes.name();
    }
    if (changes.isProvided("minThreshold")) {
      this.minThreshold = changes.minThreshold();
    }
    this.updatedAt = now;
  }

  MedicationResponse toResponse() {
    boolean lowStock = minThreshold != null && quantity < minThreshold;
    return new MedicationResponse(id, name, quantity, minThreshold, lowStock, createdAt, updatedAt);
  }
}
