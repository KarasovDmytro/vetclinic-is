package ua.vetclinic.api.common;

/** Тіло PATCH-запиту без жодного поля (minProperties: 1). */
public class EmptyBodyException extends RuntimeException {

  /** Створює виняток для порожнього тіла PATCH-запиту. */
  public EmptyBodyException() {
    super("Request body has no properties");
  }
}
