package ua.vetclinic.api.appointments;

import java.util.UUID;
import org.springframework.stereotype.Component;
import ua.vetclinic.api.patients.PatientArchiveGuard;

/**
 * Забороняє архівувати картку пацієнта, поки в нього є записи у статусі «Заплановано» (AD-04.2).
 *
 * <p>Реалізує {@link PatientArchiveGuard} з боку модуля записів, тому пакет {@code patients} не
 * залежить від {@code appointments}.
 */
@Component
class AppointmentArchiveGuard implements PatientArchiveGuard {

  private final AppointmentRepository appointmentRepository;

  AppointmentArchiveGuard(AppointmentRepository appointmentRepository) {
    this.appointmentRepository = appointmentRepository;
  }

  @Override
  public boolean hasScheduledAppointments(UUID patientId) {
    return appointmentRepository.existsByPatientIdAndStatus(patientId, AppointmentStatus.SCHEDULED);
  }
}
