package ua.vetclinic.api.appointments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-ресурс записів на прийом {@code /api/v1/appointments}: створення, перегляд, перенесення та
 * скасування.
 *
 * <p>Змінювати записи може лише адміністратор, переглядати також лікар (див. {@code
 * SecurityConfig}). Бізнес-перевірки виконує {@link AppointmentService}.
 */
@RestController
@RequestMapping("/api/v1/appointments")
class AppointmentController {

  private final AppointmentService appointmentService;

  AppointmentController(AppointmentService appointmentService) {
    this.appointmentService = appointmentService;
  }

  /**
   * Створює запис на вільний слот лікаря (операція {@code createAppointment}, REQ-F-021–023,
   * REQ-F-049).
   *
   * @param request лікар, пацієнт і початок слота
   * @return 201 зі створеним записом у статусі {@code SCHEDULED} і заголовком {@code Location}
   */
  @PostMapping
  ResponseEntity<AppointmentResponse> createAppointment(
      @Valid @RequestBody CreateAppointmentRequest request) {
    AppointmentResponse body = appointmentService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/appointments/" + body.id())).body(body);
  }

  /**
   * Повертає сторінку записів із фільтрами (операція {@code listAppointments}, REQ-F-020,
   * REQ-F-026).
   *
   * @param doctorId лише записи цього лікаря, якщо задано
   * @param patientId лише записи цього пацієнта, якщо задано
   * @param status лише записи в цьому статусі, якщо задано
   * @param from перший день діапазону (включно) за часом клініки
   * @param to останній день діапазону (включно) за часом клініки
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка записів, відсортованих за часом початку
   */
  @GetMapping
  AppointmentPage listAppointments(
      @RequestParam(required = false) UUID doctorId,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) AppointmentStatus status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return appointmentService.list(doctorId, patientId, status, from, to, page, pageSize);
  }

  /**
   * Повертає запис за ідентифікатором (операція {@code getAppointment}, REQ-F-021).
   *
   * @param appointmentId ідентифікатор запису
   * @return запис на прийом
   */
  @GetMapping("/{appointmentId}")
  AppointmentResponse getAppointment(@PathVariable UUID appointmentId) {
    return appointmentService.get(appointmentId);
  }

  /**
   * Переносить запланований запис на інший слот того самого лікаря (операція {@code
   * rescheduleAppointment}, REQ-F-023, REQ-F-024, REQ-F-049).
   *
   * @param appointmentId ідентифікатор запису
   * @param request новий початок слота
   * @return запис із новим часом
   */
  @PatchMapping("/{appointmentId}")
  AppointmentResponse rescheduleAppointment(
      @PathVariable UUID appointmentId, @Valid @RequestBody UpdateAppointmentRequest request) {
    return appointmentService.reschedule(appointmentId, request);
  }

  /**
   * Скасовує запланований запис і звільняє слот (операція {@code cancelAppointment}, REQ-F-025).
   *
   * @param appointmentId ідентифікатор запису
   * @return запис у статусі {@code CANCELLED}
   */
  @PostMapping("/{appointmentId}/cancel")
  AppointmentResponse cancelAppointment(@PathVariable UUID appointmentId) {
    return appointmentService.cancel(appointmentId);
  }
}
