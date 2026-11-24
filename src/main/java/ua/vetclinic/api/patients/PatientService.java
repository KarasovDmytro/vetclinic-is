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
import ua.vetclinic.api.common.Strings;
import ua.vetclinic.api.owners.OwnerLookup;

/**
 * Бізнес-логіка пацієнтів: реєстрація з прив'язкою до власника, редагування базових даних,
 * архівація картки та пошук.
 *
 * <p>Перед архівацією опитуються всі {@link PatientArchiveGuard}; поки є заплановані записи,
 * архівація заборонена (AD-04.2).
 */
@Service
class PatientService {

  private final PatientRepository patientRepository;

  private final OwnerLookup ownerLookup;

  private final AuditLog auditLog;

  private final List<PatientArchiveGuard> archiveGuards;

  PatientService(
      PatientRepository patientRepository,
      OwnerLookup ownerLookup,
      AuditLog auditLog,
      List<PatientArchiveGuard> archiveGuards) {
    this.patientRepository = patientRepository;
    this.ownerLookup = ownerLookup;
    this.auditLog = auditLog;
    this.archiveGuards = archiveGuards;
  }

  /**
   * Реєструє пацієнта з активною карткою.
   *
   * @param request дані тварини та власник
   * @return створений пацієнт
   * @throws RequestValidationException якщо власника не знайдено або дата народження в майбутньому
   */
  PatientResponse create(CreatePatientRequest request) {
    List<ErrorDetail> errors = new ArrayList<>();
    if (!ownerLookup.exists(request.ownerId())) {
      errors.add(new ErrorDetail("ownerId", "REFERENCE_NOT_FOUND", "Власника не знайдено"));
    }
    addFutureBirthDateError(errors, request.birthDate());
    rejectIfAny(errors, "PATIENT_CREATED", null);
    OffsetDateTime now = OffsetDateTime.now();
    PatientEntity patient =
        new PatientEntity(
            UUID.randomUUID(),
            request.ownerId(),
            request.name(),
            request.species(),
            request.breed(),
            request.sex(),
            request.birthDate(),
            PatientStatus.ACTIVE,
            now,
            now);
    PatientResponse created = patientRepository.save(patient).toResponse();
    auditLog.success("PATIENT_CREATED", "PATIENT", created.id());
    return created;
  }

  /**
   * Повертає пацієнта за ідентифікатором.
   *
   * @param patientId ідентифікатор пацієнта
   * @return пацієнт
   * @throws ResourceNotFoundException якщо пацієнта немає
   */
  PatientResponse get(UUID patientId) {
    return findOrThrow(patientId).toResponse();
  }

  /**
   * Частково оновлює базові дані пацієнта.
   *
   * @param patientId ідентифікатор пацієнта
   * @param request передані поля
   * @return оновлений пацієнт
   * @throws EmptyBodyException якщо тіло не містить жодного поля
   * @throws RequestValidationException якщо передано {@code ownerId} чи {@code status}, обов'язкове
   *     поле {@code null} або дата народження в майбутньому
   * @throws ResourceNotFoundException якщо пацієнта немає
   */
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

  /**
   * Переводить картку пацієнта в статус «Архівна».
   *
   * @param patientId ідентифікатор пацієнта
   * @return пацієнт зі статусом {@code ARCHIVED}
   * @throws ResourceNotFoundException якщо пацієнта немає
   * @throws ConflictException {@code PATIENT_ARCHIVED}, якщо картку вже архівовано, або {@code
   *     PATIENT_HAS_SCHEDULED_APPOINTMENTS}, якщо є заплановані записи
   */
  PatientResponse archive(UUID patientId) {
    PatientEntity patient = findOrThrow(patientId);
    if (patient.isArchived()) {
      auditLog.failure("PATIENT_ARCHIVED", "PATIENT", patientId, "PATIENT_ARCHIVED");
      throw new ConflictException("PATIENT_ARCHIVED", "Картку пацієнта архівовано");
    }
    if (archiveGuards.stream().anyMatch(guard -> guard.hasScheduledAppointments(patientId))) {
      auditLog.failure(
          "PATIENT_ARCHIVED", "PATIENT", patientId, "PATIENT_HAS_SCHEDULED_APPOINTMENTS");
      throw new ConflictException(
          "PATIENT_HAS_SCHEDULED_APPOINTMENTS", "У пацієнта є заплановані записи");
    }
    patient.archive(OffsetDateTime.now());
    PatientResponse archived = patientRepository.save(patient).toResponse();
    auditLog.success("PATIENT_ARCHIVED", "PATIENT", patientId);
    return archived;
  }

  /**
   * Повертає сторінку пацієнтів за фільтрами.
   *
   * @param query підрядок клички або {@code null}
   * @param ownerId фільтр за власником або {@code null}
   * @param status фільтр за статусом картки або {@code null}
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки
   * @return сторінка пацієнтів, відсортованих за кличкою
   */
  PatientPage list(String query, UUID ownerId, PatientStatus status, int page, int pageSize) {
    Specification<PatientEntity> filter = Specification.unrestricted();
    if (query != null) {
      String pattern = "%" + Strings.escapeLike(query.toLowerCase(Locale.ROOT)) + "%";
      filter = filter.and((root, q, cb) -> cb.like(cb.lower(root.get("name")), pattern, '\\'));
    }
    if (ownerId != null) {
      filter = filter.and((root, q, cb) -> cb.equal(root.get("ownerId"), ownerId));
    }
    if (status != null) {
      filter = filter.and((root, q, cb) -> cb.equal(root.get("status"), status));
    }
    Page<PatientEntity> patients =
        patientRepository.findAll(
            filter, PageRequest.of(page - 1, pageSize, Sort.by("name", "id")));
    return new PatientPage(
        page,
        pageSize,
        patients.getTotalElements(),
        patients.getContent().stream().map(PatientEntity::toResponse).toList());
  }

  private PatientEntity findOrThrow(UUID patientId) {
    return patientRepository
        .findById(patientId)
        .orElseThrow(() -> new ResourceNotFoundException("Patient not found: " + patientId));
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
