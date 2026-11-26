package ua.vetclinic.api.common;

/** Нормалізація текстових полів запиту (AD-17.1: пробіли по краях обрізаються). */
public final class Strings {

  private Strings() {}

  /**
   * Обрізає пробіли по краях рядка.
   *
   * @param value значення поля або {@code null}
   * @return обрізаний рядок або {@code null}
   */
  public static String strip(String value) {
    return value == null ? null : value.strip();
  }

  /**
   * Екранує спецсимволи шаблону {@code LIKE} (символ екранування — зворотна скісна риска).
   *
   * @param value рядок пошуку
   * @return рядок, у якому {@code %}, {@code _} і {@code \} шукаються буквально
   */
  public static String escapeLike(String value) {
    return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}
