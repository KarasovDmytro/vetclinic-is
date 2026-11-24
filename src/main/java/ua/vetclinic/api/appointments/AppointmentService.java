package ua.vetclinic.api.appointments;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.ClinicZone;
import ua.vetclinic.api.common.ConflictException;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.common.ResourceNotFoundException;
import ua.vetclinic.api.patients.PatientInfo;
import ua.vetclinic.api.patients.PatientLookup;
import ua.vetclinic.api.users.DoctorInfo;
import ua.vetclinic.api.users.Doctors;

/**
 * Бізнес-логіка записів на прийом: перевірка слота, лікаря й пацієнта, захист від конфлікту
 * розкладу та фіксація кожної зміни в журналі аудиту.
 *
 * <p>Конфлікт розкладу перевіряється двічі: запитом перед збереженням і унікальним індексом БД,
 * який ловить паралельні запити на той самий слот.
 */
@Service
class AppointmentService {

  /** Слот зайнятий записами у цих статусах; скасований запис слот звільняє (AD-03.1). */
  static final Set<AppointmentStatus> OCCUPYING =
      EnumSet.of(AppointmentStatus.SCHEDULED, AppointmentStatus.COMPLETED);

  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

  private final AppointmentRepository appointmentRepository;

  private final Doctors doctors;

  private final PatientLookup patients;

  private final AuditLog auditLog;

  AppointmentService(
      AppointmentRepository appointmentRepository,
      Doctors doctors,
      PatientLookup patients,
      AuditLog auditLog) {
    this.appointmentRepository = appointmentRepository;
    this.doctors = doctors;
    this.patients = patients;
    this.auditLog = auditLog;
  }

  /**
   * Створює запис у статусі «Заплановано».
   *
   * @param request лікар, пацієнт і початок слота
   * @return створений запис
   * @throws RequestValidationException якщо лікаря чи пацієнта не знайдено, лікаря деактивовано або
   *     слот не відповідає сітці, робочим дням чи годинам клініки (422)
   * @throws ConflictException {@code PATIENT_ARCHIVED} для архівної картки або {@code SLOT_TAKEN},
   *     якщо слот уже зайнятий (409)
   */
  AppointmentResponse create(CreateAppointmentRequest request) {
    ZonedDateTime start = request.startsAt().atZoneSameInstant(ClinicZone.ZONE);
    List<ErrorDetail> errors = new ArrayList<>();
    Optional<DoctorInfo> doctor = doctors.find(request.doctorId());
    if (doctor.isEmpty()) {
      errors.add(new ErrorDetail("doctorId", "REFERENCE_NOT_FOUND", "Лікаря не знайдено"));
    } else if (!doctor.get().active()) {
      errors.add(new ErrorDetail("doctorId", "DOCTOR_NOT_AVAILABLE", "Лікаря деактивовано"));
    }
    Optional<PatientInfo> patient = patients.find(request.patientId());
    if (patient.isEmpty()) {
      errors.add(new ErrorDetail("patientId", "REFERENCE_NOT_FOUND", "Пацієнта не знайдено"));
    }
    ClinicSchedule.slotError(start).ifPresent(errors::add);
    if (!errors.isEmpty()) {
      throw failed(
          "APPOINTMENT_CREATED", null, VALIDATION_ERROR, new RequestValidationException(errors));
    }
    if (patient.get().archived()) {
      throw failed(
          "APPOINTMENT_CREATED",
          null,
          "PATIENT_ARCHIVED",
          new ConflictException("PATIENT_ARCHIVED", "Картку пацієнта архівовано"));
    }
    OffsetDateTime startsAt = start.toOffsetDateTime();
    if (appointmentRepository.existsByDoctorIdAndStartsAtAndStatusIn(
        request.doctorId(), startsAt, OCCUPYING)) {
      throw failed("APPOINTMENT_CREATED", null, "SLOT_TAKEN", slotTaken());
    }
    OffsetDateTime now = OffsetDateTime.now();
    AppointmentEntity appointment =
        new AppointmentEntity(
            UUID.randomUUID(),
            request.doctorId(),
            request.patientId(),
            startsAt,
            start.plus(ClinicSchedule.SLOT).toOffsetDateTime(),
            AppointmentStatus.SCHEDULED,
            now,
            now);
    AppointmentResponse created;
    try {
      created = appointmentRepository.saveAndFlush(appointment).toResponse();
    } catch (DataIntegrityViolationException exception) {
      throw failed("APPOINTMENT_CREATED", null, "SLOT_TAKEN", slotTaken());
    }
    auditLog.success("APPOINTMENT_CREATED", "APPOINTMENT", created.id());
    return created;
  }

  /**
   * Повертає запис за ідентифікатором.
   *
   * @param appointmentId ідентифікатор запису
   * @return запис
   * @throws ResourceNotFoundException якщо запису немає
   */
  AppointmentResponse get(UUID appointmentId) {
    return findOrThrow(appointmentId).toResponse();
  }

  /**
   * Переносить запис на інший слот того самого лікаря.
   *
   * @param appointmentId ідентифікатор запису
   * @param request новий початок слота
   * @return запис із новим часом
   * @throws RequestValidationException якщо новий слот некоректний або лікаря деактивовано (422)
   * @throws ResourceNotFoundException якщо запису немає
   * @throws ConflictException {@code APPOINTMENT_NOT_SCHEDULED}, якщо запис не в статусі
   *     «Заплановано», або {@code SLOT_TAKEN}, якщо новий слот зайнятий (409)
   */
  AppointmentResponse reschedule(UUID appointmentId, UpdateAppointmentRequest request) {
    ZonedDateTime start = request.startsAt().atZoneSameInstant(ClinicZone.ZONE);
    Optional<ErrorDetail> slotError = ClinicSchedule.slotError(start);
    if (slotError.isPresent()) {
      throw failed(
          "APPOINTMENT_RESCHEDULED",
          appointmentId,
          VALIDATION_ERROR,
          new RequestValidationException(List.of(slotError.get())));
    }
    AppointmentEntity appointment = findOrThrow(appointmentId);
    if (!doctors.find(appointment.doctorId()).map(DoctorInfo::active).orElse(false)) {
      throw failed(
          "APPOINTMENT_RESCHEDULED",
          appointmentId,
          VALIDATION_ERROR,
          new RequestValidationException(
              List.of(new ErrorDetail("doctorId", "DOCTOR_NOT_AVAILABLE", "Лікаря деактивовано"))));
    }
    requireScheduled(appointment, "APPOINTMENT_RESCHEDULED");
    OffsetDateTime startsAt = start.toOffsetDateTime();
    if (appointmentRepository.existsByDoctorIdAndStartsAtAndStatusIn(
        appointment.doctorId(), startsAt, OCCUPYING)) {
      throw failed("APPOINTMENT_RESCHEDULED", appointmentId, "SLOT_TAKEN", slotTaken());
    }
    appointment.reschedule(
        startsAt, start.plus(ClinicSchedule.SLOT).toOffsetDateTime(), OffsetDateTime.now());
    AppointmentResponse rescheduled;
    try {
      rescheduled = appointmentRepository.saveAndFlush(appointment).toResponse();
    } catch (DataIntegrityViolationException exception) {
      throw failed("APPOINTMENT_RESCHEDULED", appointmentId, "SLOT_TAKEN", slotTaken());
    }
    auditLog.success("APPOINTMENT_RESCHEDULED", "APPOINTMENT", appointmentId);
    return rescheduled;
  }

  /**
   * Скасовує запис; слот звільняється для нових записів.
   *
   * @param appointmentId ідентифікатор запису
   * @return скасований запис
   * @throws ResourceNotFoundException якщо запису немає
   * @throws ConflictException {@code APPOINTMENT_NOT_SCHEDULED}, якщо запис уже завершено чи
   *     скасовано
   */
  AppointmentResponse cancel(UUID appointmentId) {
    AppointmentEntity appointment = findOrThrow(appointmentId);
    requireScheduled(appointment, "APPOINTMENT_CANCELLED");
    appointment.cancel(OffsetDateTime.now());
    AppointmentResponse cancelled = appointmentRepository.save(appointment).toResponse();
    auditLog.success("APPOINTMENT_CANCELLED", "APPOINTMENT", appointmentId);
    return cancelled;
  }

  /**
   * Повертає сторінку записів за фільтрами; дні діапазону рахуються за часом клініки.
   *
   * @param doctorId фільтр за лікарем або {@code null}
   * @param patientId фільтр за пацієнтом або {@code null}
   * @param status фільтр за статусом або {@code null}
   * @param from перший день діапазону або {@code null}
   * @param to останній день діапазону або {@code null}
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки
   * @return сторінка записів, відсортованих за часом початку
   * @throws RequestValidationException якщо {@code to} раніше за {@code from}
   */
  AppointmentPage list(
      UUID doctorId,
      UUID patientId,
      AppointmentStatus status,
      LocalDate from,
      LocalDate to,
      int page,
      int pageSize) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new RequestValidationException(
          List.of(
              new ErrorDetail("to", "INVALID_RANGE", "Дата завершення раніше за дату початку")));
    }
    Specification<AppointmentEntity> filter = Specification.unrestricted();
    if (doctorId != null) {
      filter = filter.and((root, q, cb) -> cb.equal(root.get("doctorId"), doctorId));
    }
    if (patientId != null) {
      filter = filter.and((root, q, cb) -> cb.equal(root.get("patientId"), patientId));
    }
    if (status != null) {
      filter = filter.and((root, q, cb) -> cb.equal(root.get("status"), status));
    }
    if (from != null) {
      OffsetDateTime rangeStart = from.atStartOfDay(ClinicZone.ZONE).toOffsetDateTime();
      filter =
          filter.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("startsAt"), rangeStart));
    }
    if (to != null) {
      OffsetDateTime rangeEnd = to.plusDays(1).atStartOfDay(ClinicZone.ZONE).toOffsetDateTime();
      filter = filter.and((root, q, cb) -> cb.lessThan(root.get("startsAt"), rangeEnd));
    }
    Page<AppointmentEntity> appointments =
        appointmentRepository.findAll(
            filter, PageRequest.of(page - 1, pageSize, Sort.by("startsAt", "id")));
    return new AppointmentPage(
        page,
        pageSize,
        appointments.getTotalElements(),
        appointments.getContent().stream().map(AppointmentEntity::toResponse).toList());
  }

  private AppointmentEntity findOrThrow(UUID appointmentId) {
    return appointmentRepository
        .findById(appointmentId)
        .orElseThrow(
            () -> new ResourceNotFoundException("Appointment not found: " + appointmentId));
  }

  private void requireScheduled(AppointmentEntity appointment, String event) {
    if (!appointment.isScheduled()) {
      throw failed(
          event,
          appointment.id(),
          "APPOINTMENT_NOT_SCHEDULED",
          new ConflictException("APPOINTMENT_NOT_SCHEDULED", "Запис не у статусі «Заплановано»"));
    }
  }

  /** Фіксує відхилену дію в журналі аудиту (AD-22.4) і повертає виняток для передачі далі. */
  private <E extends RuntimeException> E failed(
      String event, UUID entityId, String errorCode, E exception) {
    auditLog.failure(event, "APPOINTMENT", entityId, errorCode);
    return exception;
  }

  private static ConflictException slotTaken() {
    return new ConflictException("SLOT_TAKEN", "Слот вже зайнятий");
  }
}
