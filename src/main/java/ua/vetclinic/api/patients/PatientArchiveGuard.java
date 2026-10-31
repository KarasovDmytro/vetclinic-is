package ua.vetclinic.api.patients;

import java.util.UUID;

/**
 * Перевірка перед архівуванням картки: інший модуль може заборонити архівацію, поки є заплановані записи
 * (AD-04.2). Залежність спрямована від реалізації до patients, а не навпаки.
 */
public interface PatientArchiveGuard {

	boolean hasScheduledAppointments(UUID patientId);
}
