package ua.vetclinic.api.patients;

import java.util.UUID;

class PatientNotFoundException extends RuntimeException {

	PatientNotFoundException(UUID patientId) {
		super("Patient not found: " + patientId);
	}
}
