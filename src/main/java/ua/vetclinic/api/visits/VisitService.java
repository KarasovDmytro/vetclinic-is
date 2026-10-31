package ua.vetclinic.api.visits;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ua.vetclinic.api.appointments.AppointmentInfo;
import ua.vetclinic.api.appointments.Appointments;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.ConflictException;
import ua.vetclinic.api.common.EmptyBodyException;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.ForbiddenException;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.common.ResourceNotFoundException;
import ua.vetclinic.api.medications.MedicationStock;
import ua.vetclinic.api.medications.StockRequest;
import ua.vetclinic.api.medications.WithdrawnMedication;
import ua.vetclinic.api.notifications.LowStockNotifier;
import ua.vetclinic.api.patients.PatientLookup;

@Service
class VisitService {

	private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

	private static final String PATIENT_ARCHIVED = "PATIENT_ARCHIVED";

	private final VisitRepository visitRepository;

	private final Appointments appointments;

	private final PatientLookup patients;

	private final MedicationStock medicationStock;

	private final LowStockNotifier lowStockNotifier;

	private final AuditLog auditLog;

	VisitService(VisitRepository visitRepository, Appointments appointments, PatientLookup patients,
			MedicationStock medicationStock, LowStockNotifier lowStockNotifier, AuditLog auditLog) {
		this.visitRepository = visitRepository;
		this.appointments = appointments;
		this.patients = patients;
		this.medicationStock = medicationStock;
		this.lowStockNotifier = lowStockNotifier;
		this.auditLog = auditLog;
	}

	@Transactional
	VisitResponse create(UUID doctorId, UUID appointmentId, CreateVisitRequest request) {
		List<ErrorDetail> referenceErrors = medicationErrors(request.medications());
		if (!referenceErrors.isEmpty()) {
			throw failed("VISIT_CREATED", null, VALIDATION_ERROR, new RequestValidationException(referenceErrors));
		}
		AppointmentInfo appointment = appointments.findForUpdate(appointmentId)
				.orElseThrow(() -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
		if (!appointment.doctorId().equals(doctorId)) {
			throw failed("VISIT_CREATED", null, "NOT_ASSIGNED_DOCTOR",
					new ForbiddenException("NOT_ASSIGNED_DOCTOR", "Запис призначено іншому лікарю"));
		}
		if (!appointment.scheduled()) {
			throw failed("VISIT_CREATED", null, "APPOINTMENT_NOT_SCHEDULED",
					new ConflictException("APPOINTMENT_NOT_SCHEDULED", "Запис не у статусі «Заплановано»"));
		}
		if (patients.find(appointment.patientId()).orElseThrow().archived()) {
			throw failed("VISIT_CREATED", null, PATIENT_ARCHIVED, patientArchived());
		}
		List<WithdrawnMedication> withdrawn;
		try {
			withdrawn = medicationStock.withdraw(request.medications().stream()
					.map(item -> new StockRequest(item.medicationId(), item.quantity())).toList());
		}
		catch (ConflictException exception) {
			throw failed("VISIT_CREATED", null, exception.code(), exception);
		}
		VisitEntity visit = new VisitEntity(UUID.randomUUID(), appointmentId, appointment.patientId(), doctorId,
				appointment.startsAt(), request.diagnosis(), request.prescriptions(), request.notes(),
				withdrawn.stream().map(item -> new VisitMedicationEntry(item.medicationId(), item.name(),
						item.quantity())).toList(),
				OffsetDateTime.now());
		VisitResponse created = visitRepository.save(visit).toResponse();
		appointments.complete(appointmentId, created.id());
		withdrawn.stream().filter(WithdrawnMedication::isLow).forEach(item -> lowStockNotifier
				.notifyLowStock(item.medicationId(), item.name(), item.remaining(), item.minThreshold()));
		auditLog.success("VISIT_CREATED", "VISIT", created.id());
		return created;
	}

	@Transactional(readOnly = true)
	VisitResponse get(UUID visitId) {
		return findOrThrow(visitId).toResponse();
	}

	@Transactional
	VisitResponse update(UUID doctorId, UUID visitId, UpdateVisitRequest request) {
		if (request.isEmpty()) {
			throw failed("VISIT_UPDATED", visitId, VALIDATION_ERROR, new EmptyBodyException());
		}
		List<ErrorDetail> violations = request.violations();
		if (!violations.isEmpty()) {
			throw failed("VISIT_UPDATED", visitId, VALIDATION_ERROR, new RequestValidationException(violations));
		}
		VisitEntity visit = findOrThrow(visitId);
		if (!visit.doctorId().equals(doctorId)) {
			throw failed("VISIT_UPDATED", visitId, "NOT_VISIT_AUTHOR",
					new ForbiddenException("NOT_VISIT_AUTHOR", "Прийом створено іншим лікарем"));
		}
		if (patients.find(visit.patientId()).orElseThrow().archived()) {
			throw failed("VISIT_UPDATED", visitId, PATIENT_ARCHIVED, patientArchived());
		}
		visit.apply(request, OffsetDateTime.now());
		VisitResponse updated = visitRepository.save(visit).toResponse();
		auditLog.success("VISIT_UPDATED", "VISIT", visitId);
		return updated;
	}

	@Transactional(readOnly = true)
	VisitPage listByPatient(UUID patientId, int page, int pageSize) {
		if (patients.find(patientId).isEmpty()) {
			throw new ResourceNotFoundException("Patient not found: " + patientId);
		}
		Page<VisitEntity> visits = visitRepository.findByPatientId(patientId, PageRequest.of(page - 1, pageSize,
				Sort.by(Sort.Direction.DESC, "visitDate").and(Sort.by("id"))));
		return new VisitPage(page, pageSize, visits.getTotalElements(),
				visits.getContent().stream().map(VisitEntity::toResponse).toList());
	}

	private List<ErrorDetail> medicationErrors(List<VisitMedicationInput> items) {
		Set<UUID> missing = medicationStock.missing(items.stream().map(VisitMedicationInput::medicationId).toList());
		Set<UUID> seen = new HashSet<>();
		List<ErrorDetail> errors = new ArrayList<>();
		for (int i = 0; i < items.size(); i++) {
			UUID medicationId = items.get(i).medicationId();
			String field = "medications[" + i + "].medicationId";
			if (!seen.add(medicationId)) {
				errors.add(new ErrorDetail(field, "DUPLICATE_MEDICATION", "Препарат уже є в переліку"));
			}
			else if (missing.contains(medicationId)) {
				errors.add(new ErrorDetail(field, "REFERENCE_NOT_FOUND", "Препарат не знайдено в каталозі"));
			}
		}
		return errors;
	}

	private VisitEntity findOrThrow(UUID visitId) {
		return visitRepository.findById(visitId)
				.orElseThrow(() -> new ResourceNotFoundException("Visit not found: " + visitId));
	}

	private static ConflictException patientArchived() {
		return new ConflictException(PATIENT_ARCHIVED, "Картку пацієнта архівовано");
	}

	/** Фіксує відхилену дію в журналі аудиту (AD-22.4) і повертає виняток для передачі далі. */
	private <E extends RuntimeException> E failed(String event, UUID entityId, String errorCode, E exception) {
		auditLog.failure(event, "VISIT", entityId, errorCode);
		return exception;
	}
}
