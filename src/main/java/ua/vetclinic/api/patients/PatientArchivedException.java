package ua.vetclinic.api.patients;

import java.util.UUID;

class PatientArchivedException extends RuntimeException {

	PatientArchivedException(UUID patientId) {
		super("Patient is already archived: " + patientId);
	}
}
