package ua.vetclinic.api.patients;

import java.util.UUID;

/**
 * Перевірка перед архівуванням картки: інший модуль може заборонити архівацію, поки є заплановані
 * записи (AD-04.2). Залежність спрямована від реалізації до patients, а не навпаки.
 */
public interface PatientArchiveGuard {

  /**
   * Перевіряє, чи є в пацієнта записи у статусі «Заплановано».
   *
   * @param patientId ідентифікатор пацієнта
   * @return {@code true}, якщо архівацію потрібно заборонити
   */
  boolean hasScheduledAppointments(UUID patientId);
}
