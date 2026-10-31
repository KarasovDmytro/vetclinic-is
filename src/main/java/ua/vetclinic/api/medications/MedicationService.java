package ua.vetclinic.api.medications;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.ConflictException;
import ua.vetclinic.api.common.EmptyBodyException;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.common.ResourceNotFoundException;
import ua.vetclinic.api.common.Strings;

@Service
class MedicationService {

	private static final int MAX_QUANTITY = 1_000_000;

	private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

	private static final String NAME_TAKEN = "MEDICATION_NAME_TAKEN";

	private final MedicationRepository medicationRepository;

	private final AuditLog auditLog;

	MedicationService(MedicationRepository medicationRepository, AuditLog auditLog) {
		this.medicationRepository = medicationRepository;
		this.auditLog = auditLog;
	}

	MedicationResponse create(CreateMedicationRequest request) {
		if (medicationRepository.existsByNameIgnoreCase(request.name())) {
			throw failed("MEDICATION_CREATED", null, NAME_TAKEN, nameTaken());
		}
		MedicationEntity medication = new MedicationEntity(UUID.randomUUID(), request.name(), request.quantity(),
				request.minThreshold(), OffsetDateTime.now());
		MedicationResponse created = medicationRepository.save(medication).toResponse();
		auditLog.success("MEDICATION_CREATED", "MEDICATION", created.id());
		return created;
	}

	MedicationResponse get(UUID medicationId) {
		return findOrThrow(medicationId).toResponse();
	}

	MedicationResponse update(UUID medicationId, UpdateMedicationRequest request) {
		if (request.isEmpty()) {
			throw failed("MEDICATION_UPDATED", medicationId, VALIDATION_ERROR, new EmptyBodyException());
		}
		List<ErrorDetail> violations = request.violations();
		if (!violations.isEmpty()) {
			throw failed("MEDICATION_UPDATED", medicationId, VALIDATION_ERROR,
					new RequestValidationException(violations));
		}
		MedicationEntity medication = findOrThrow(medicationId);
		if (request.isProvided("name")
				&& medicationRepository.existsByNameIgnoreCaseAndIdNot(request.name(), medicationId)) {
			throw failed("MEDICATION_UPDATED", medicationId, NAME_TAKEN, nameTaken());
		}
		medication.apply(request, OffsetDateTime.now());
		MedicationResponse updated = medicationRepository.save(medication).toResponse();
		auditLog.success("MEDICATION_UPDATED", "MEDICATION", medicationId);
		return updated;
	}

	@Transactional
	MedicationResponse adjustStock(UUID medicationId, StockAdjustmentRequest request) {
		int delta = request.delta();
		if (delta == 0) {
			throw failed("MEDICATION_STOCK_ADJUSTED", medicationId, VALIDATION_ERROR, new RequestValidationException(
					List.of(new ErrorDetail("delta", "ZERO_DELTA", "Зміна не може дорівнювати нулю"))));
		}
		MedicationEntity medication = medicationRepository.findByIdForUpdate(medicationId)
				.orElseThrow(() -> new ResourceNotFoundException("Medication not found: " + medicationId));
		long result = (long) medication.quantity() + delta;
		if (result > MAX_QUANTITY) {
			throw failed("MEDICATION_STOCK_ADJUSTED", medicationId, VALIDATION_ERROR, new RequestValidationException(
					List.of(new ErrorDetail("delta", "OUT_OF_RANGE", "Залишок перевищує 1 000 000"))));
		}
		if (result < 0) {
			throw failed("MEDICATION_STOCK_ADJUSTED", medicationId, "NEGATIVE_STOCK",
					new ConflictException("NEGATIVE_STOCK", "Залишок не може бути від'ємним"));
		}
		medication.adjust(delta, OffsetDateTime.now());
		MedicationResponse adjusted = medicationRepository.save(medication).toResponse();
		auditLog.success("MEDICATION_STOCK_ADJUSTED", "MEDICATION", medicationId);
		return adjusted;
	}

	MedicationPage list(String query, Boolean lowStock, int page, int pageSize) {
		Specification<MedicationEntity> filter = Specification.unrestricted();
		if (query != null) {
			String pattern = "%" + Strings.escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
			filter = filter.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
		}
		if (lowStock != null) {
			boolean wanted = lowStock;
			filter = filter.and((root, q, cb) -> {
				Predicate low = cb.and(cb.isNotNull(root.get("minThreshold")),
						cb.lessThan(root.<Integer>get("quantity"), root.<Integer>get("minThreshold")));
				return wanted ? low : cb.not(low);
			});
		}
		Page<MedicationEntity> medications = medicationRepository.findAll(filter,
				PageRequest.of(page - 1, pageSize, Sort.by("name")));
		return new MedicationPage(page, pageSize, medications.getTotalElements(),
				medications.getContent().stream().map(MedicationEntity::toResponse).toList());
	}

	private MedicationEntity findOrThrow(UUID medicationId) {
		return medicationRepository.findById(medicationId)
				.orElseThrow(() -> new ResourceNotFoundException("Medication not found: " + medicationId));
	}

	private static ConflictException nameTaken() {
		return new ConflictException(NAME_TAKEN, "Медикамент з такою назвою вже існує");
	}

	/** Фіксує відхилену дію в журналі аудиту (AD-22.4) і повертає виняток для передачі далі. */
	private <E extends RuntimeException> E failed(String event, UUID entityId, String errorCode, E exception) {
		auditLog.failure(event, "MEDICATION", entityId, errorCode);
		return exception;
	}
}
