package ua.vetclinic.api.patients;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.common.ClinicZone;
import ua.vetclinic.api.common.EmptyBodyException;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.owners.OwnerLookup;

@Service
class PatientService {

	private final PatientRepository patientRepository;

	private final OwnerLookup ownerLookup;

	PatientService(PatientRepository patientRepository, OwnerLookup ownerLookup) {
		this.patientRepository = patientRepository;
		this.ownerLookup = ownerLookup;
	}

	PatientResponse create(CreatePatientRequest request) {
		List<ErrorDetail> errors = new ArrayList<>();
		if (!ownerLookup.exists(request.ownerId())) {
			errors.add(new ErrorDetail("ownerId", "REFERENCE_NOT_FOUND", "Власника не знайдено"));
		}
		addFutureBirthDateError(errors, request.birthDate());
		throwIfAny(errors);
		OffsetDateTime now = OffsetDateTime.now();
		PatientEntity patient = new PatientEntity(UUID.randomUUID(), request.ownerId(), request.name(),
				request.species(), request.breed(), request.sex(), request.birthDate(), PatientStatus.ACTIVE, now,
				now);
		return patientRepository.save(patient).toResponse();
	}

	PatientResponse get(UUID patientId) {
		return findOrThrow(patientId).toResponse();
	}

	PatientResponse update(UUID patientId, UpdatePatientRequest request) {
		if (request.isEmpty()) {
			throw new EmptyBodyException();
		}
		List<ErrorDetail> errors = new ArrayList<>(request.violations());
		addFutureBirthDateError(errors, request.birthDate());
		throwIfAny(errors);
		PatientEntity patient = findOrThrow(patientId);
		patient.apply(request, OffsetDateTime.now());
		return patientRepository.save(patient).toResponse();
	}

	PatientResponse archive(UUID patientId) {
		PatientEntity patient = findOrThrow(patientId);
		if (patient.isArchived()) {
			throw new PatientArchivedException(patientId);
		}
		patient.archive(OffsetDateTime.now());
		return patientRepository.save(patient).toResponse();
	}

	PatientPage list(String query, UUID ownerId, PatientStatus status, int page, int pageSize) {
		Specification<PatientEntity> filter = Specification.unrestricted();
		if (query != null) {
			String pattern = "%" + escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
			filter = filter.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
		}
		if (ownerId != null) {
			filter = filter.and((root, q, cb) -> cb.equal(root.get("ownerId"), ownerId));
		}
		if (status != null) {
			filter = filter.and((root, q, cb) -> cb.equal(root.get("status"), status));
		}
		Page<PatientEntity> patients = patientRepository.findAll(filter,
				PageRequest.of(page - 1, pageSize, Sort.by("name")));
		return new PatientPage(page, pageSize, patients.getTotalElements(),
				patients.getContent().stream().map(PatientEntity::toResponse).toList());
	}

	private PatientEntity findOrThrow(UUID patientId) {
		return patientRepository.findById(patientId).orElseThrow(() -> new PatientNotFoundException(patientId));
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void addFutureBirthDateError(List<ErrorDetail> errors, LocalDate birthDate) {
		if (birthDate != null && birthDate.isAfter(LocalDate.now(ClinicZone.ZONE))) {
			errors.add(new ErrorDetail("birthDate", "OUT_OF_RANGE", "Дата народження в майбутньому"));
		}
	}

	private static void throwIfAny(List<ErrorDetail> errors) {
		if (!errors.isEmpty()) {
			throw new RequestValidationException(errors);
		}
	}
}
