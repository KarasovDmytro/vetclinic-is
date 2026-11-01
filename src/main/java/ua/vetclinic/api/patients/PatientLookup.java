package ua.vetclinic.api.patients;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

/** Доступ інших модулів до пацієнтів без відкриття сховища. */
@Service
public class PatientLookup {

	private final PatientRepository patientRepository;

	PatientLookup(PatientRepository patientRepository) {
		this.patientRepository = patientRepository;
	}

	public Optional<PatientInfo> find(UUID patientId) {
		return patientRepository.findById(patientId).map(PatientEntity::toInfo);
	}

	public Map<UUID, String> namesOf(Collection<UUID> patientIds) {
		Map<UUID, String> names = new HashMap<>();
		patientRepository.findAllById(patientIds).forEach(patient -> names.put(patient.toInfo().id(), patient.toInfo().name()));
		return names;
	}
}
