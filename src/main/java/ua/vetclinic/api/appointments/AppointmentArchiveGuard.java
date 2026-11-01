package ua.vetclinic.api.appointments;

import java.util.UUID;

import org.springframework.stereotype.Component;
import ua.vetclinic.api.patients.PatientArchiveGuard;

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
