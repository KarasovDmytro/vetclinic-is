package ua.vetclinic.api.common;

/** Об'єкта, указаного в шляху запиту, не існує (відповідь 404 NOT_FOUND). */
public class ResourceNotFoundException extends RuntimeException {

  /**
   * Створює виняток.
   *
   * @param message технічний опис для журналу; клієнт отримує загальне повідомлення
   */
  public ResourceNotFoundException(String message) {
    super(message);
  }
}
