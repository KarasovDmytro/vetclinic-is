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
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.ClinicZone;
import ua.vetclinic.api.common.ConflictException;
import ua.vetclinic.api.common.EmptyBodyException;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.common.ResourceNotFoundException;
import ua.vetclinic.api.owners.OwnerLookup;

@Service
class PatientService {

	private final PatientRepository patientRepository;

	private final OwnerLookup ownerLookup;

	private final AuditLog auditLog;

	private final List<PatientArchiveGuard> archiveGuards;

	PatientService(PatientRepository patientRepository, OwnerLookup ownerLookup, AuditLog auditLog,
			List<PatientArchiveGuard> archiveGuards) {
		this.patientRepository = patientRepository;
		this.ownerLookup = ownerLookup;
		this.auditLog = auditLog;
		this.archiveGuards = archiveGuards;
	}

	PatientResponse create(CreatePatientRequest request) {
		List<ErrorDetail> errors = new ArrayList<>();
		if (!ownerLookup.exists(request.ownerId())) {
			errors.add(new ErrorDetail("ownerId", "REFERENCE_NOT_FOUND", "Власника не знайдено"));
		}
		addFutureBirthDateError(errors, request.birthDate());
		rejectIfAny(errors, "PATIENT_CREATED", null);
		OffsetDateTime now = OffsetDateTime.now();
		PatientEntity patient = new PatientEntity(UUID.randomUUID(), request.ownerId(), request.name(),
				request.species(), request.breed(), request.sex(), request.birthDate(), PatientStatus.ACTIVE, now,
				now);
		PatientResponse created = patientRepository.save(patient).toResponse();
		auditLog.success("PATIENT_CREATED", "PATIENT", created.id());
		return created;
	}

	PatientResponse get(UUID patientId) {
		return findOrThrow(patientId).toResponse();
	}

	PatientResponse update(UUID patientId, UpdatePatientRequest request) {
		if (request.isEmpty()) {
			auditLog.failure("PATIENT_UPDATED", "PATIENT", patientId, "VALIDATION_ERROR");
			throw new EmptyBodyException();
		}
		List<ErrorDetail> errors = new ArrayList<>(request.violations());
		addFutureBirthDateError(errors, request.birthDate());
		rejectIfAny(errors, "PATIENT_UPDATED", patientId);
		PatientEntity patient = findOrThrow(patientId);
		patient.apply(request, OffsetDateTime.now());
		PatientResponse updated = patientRepository.save(patient).toResponse();
		auditLog.success("PATIENT_UPDATED", "PATIENT", patientId);
		return updated;
	}

	PatientResponse archive(UUID patientId) {
		PatientEntity patient = findOrThrow(patientId);
		if (patient.isArchived()) {
			auditLog.failure("PATIENT_ARCHIVED", "PATIENT", patientId, "PATIENT_ARCHIVED");
			throw new ConflictException("PATIENT_ARCHIVED", "Картку пацієнта архівовано");
		}
		if (archiveGuards.stream().anyMatch(guard -> guard.hasScheduledAppointments(patientId))) {
			auditLog.failure("PATIENT_ARCHIVED", "PATIENT", patientId, "PATIENT_HAS_SCHEDULED_APPOINTMENTS");
			throw new ConflictException("PATIENT_HAS_SCHEDULED_APPOINTMENTS", "У пацієнта є заплановані записи");
		}
		patient.archive(OffsetDateTime.now());
		PatientResponse archived = patientRepository.save(patient).toResponse();
		auditLog.success("PATIENT_ARCHIVED", "PATIENT", patientId);
		return archived;
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
		return patientRepository.findById(patientId).orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private static void addFutureBirthDateError(List<ErrorDetail> errors, LocalDate birthDate) {
		if (birthDate != null && birthDate.isAfter(LocalDate.now(ClinicZone.ZONE))) {
			errors.add(new ErrorDetail("birthDate", "OUT_OF_RANGE", "Дата народження в майбутньому"));
		}
	}

	private void rejectIfAny(List<ErrorDetail> errors, String event, UUID entityId) {
		if (!errors.isEmpty()) {
			auditLog.failure(event, "PATIENT", entityId, "VALIDATION_ERROR");
			throw new RequestValidationException(errors);
		}
	}
}
