package ua.vetclinic.api.patients;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.Strings;

/**
 * PATCH: сеттери запам'ятовують, які поля прийшли (відсутнє поле і явний null — різні речі). Поля
 * ownerId та status приймаються лише для того, щоб відхилити їх як незмінювані (REQ-F-014).
 */
public class UpdatePatientRequest {

  private final Set<String> provided = new HashSet<>();

  private final Set<String> immutableProvided = new LinkedHashSet<>();

  @Size(min = 1, max = 100)
  private String name;

  @Size(min = 1, max = 50)
  private String species;

  @Size(min = 1, max = 100)
  private String breed;

  private Sex sex;

  private LocalDate birthDate;

  /**
   * Приймає кличку з JSON і позначає поле переданим.
   *
   * @param name нова кличка; {@code null} відхиляється як обов'язкове поле
   */
  public void setName(String name) {
    this.name = Strings.strip(name);
    provided.add("name");
  }

  /**
   * Приймає вид з JSON і позначає поле переданим.
   *
   * @param species новий вид; {@code null} відхиляється як обов'язкове поле
   */
  public void setSpecies(String species) {
    this.species = Strings.strip(species);
    provided.add("species");
  }

  /**
   * Приймає породу з JSON і позначає поле переданим.
   *
   * @param breed нова порода або {@code null}, щоб її очистити
   */
  public void setBreed(String breed) {
    this.breed = Strings.strip(breed);
    provided.add("breed");
  }

  /**
   * Приймає стать з JSON і позначає поле переданим.
   *
   * @param sex нова стать; {@code null} відхиляється як обов'язкове поле
   */
  public void setSex(Sex sex) {
    this.sex = sex;
    provided.add("sex");
  }

  /**
   * Приймає дату народження з JSON і позначає поле переданим.
   *
   * @param birthDate нова дата або {@code null}, щоб її очистити
   */
  public void setBirthDate(LocalDate birthDate) {
    this.birthDate = birthDate;
    provided.add("birthDate");
  }

  /**
   * Фіксує спробу змінити власника, щоб відхилити її з причиною {@code IMMUTABLE_FIELD}
   * (REQ-F-014).
   *
   * @param ignored значення з JSON, не використовується
   */
  public void setOwnerId(Object ignored) {
    immutableProvided.add("ownerId");
  }

  /**
   * Фіксує спробу змінити статус через PATCH, щоб відхилити її з причиною {@code IMMUTABLE_FIELD};
   * архівація виконується окремою операцією.
   *
   * @param ignored значення з JSON, не використовується
   */
  public void setStatus(Object ignored) {
    immutableProvided.add("status");
  }

  String name() {
    return name;
  }

  String species() {
    return species;
  }

  String breed() {
    return breed;
  }

  Sex sex() {
    return sex;
  }

  LocalDate birthDate() {
    return birthDate;
  }

  boolean isProvided(String field) {
    return provided.contains(field);
  }

  boolean isEmpty() {
    return provided.isEmpty() && immutableProvided.isEmpty();
  }

  List<ErrorDetail> violations() {
    List<ErrorDetail> details = new ArrayList<>();
    for (String field : immutableProvided) {
      details.add(new ErrorDetail(field, "IMMUTABLE_FIELD", "Поле не можна змінити"));
    }
    if (isProvided("name") && name == null) {
      details.add(new ErrorDetail("name", "REQUIRED", "Поле обов'язкове"));
    }
    if (isProvided("species") && species == null) {
      details.add(new ErrorDetail("species", "REQUIRED", "Поле обов'язкове"));
    }
    if (isProvided("sex") && sex == null) {
      details.add(new ErrorDetail("sex", "REQUIRED", "Поле обов'язкове"));
    }
    return details;
  }
}
