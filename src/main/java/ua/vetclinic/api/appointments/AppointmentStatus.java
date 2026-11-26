package ua.vetclinic.api.appointments;

/**
 * Статус запису на прийом (SRS §3): «Заплановано», «Завершено», «Скасовано».
 *
 * <p>Слот займають записи {@link #SCHEDULED} і {@link #COMPLETED}; скасований запис слот звільняє.
 */
public enum AppointmentStatus {
  SCHEDULED,
  COMPLETED,
  CANCELLED
}
