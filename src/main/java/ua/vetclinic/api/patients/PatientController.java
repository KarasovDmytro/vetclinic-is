package ua.vetclinic.api.patients;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.net.URI;
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
 * REST-ресурс пацієнтів {@code /api/v1/patients}.
 *
 * <p>Реєструє, редагує й архівує пацієнтів адміністратор; лікар має доступ на читання.
 */
@RestController
@RequestMapping("/api/v1/patients")
class PatientController {

  private final PatientService patientService;

  PatientController(PatientService patientService) {
    this.patientService = patientService;
  }

  /**
   * Реєструє пацієнта зі статусом {@code ACTIVE} (операція {@code createPatient}, REQ-F-012,
   * REQ-F-014).
   *
   * @param request дані тварини та власник
   * @return 201 зі створеним пацієнтом і заголовком {@code Location}
   */
  @PostMapping
  ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody CreatePatientRequest request) {
    PatientResponse body = patientService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/patients/" + body.id())).body(body);
  }

  /**
   * Шукає пацієнтів (операція {@code listPatients}, REQ-F-017, REQ-NF-003).
   *
   * @param q підрядок клички без урахування регістру, від 2 до 100 символів
   * @param ownerId лише тварини цього власника, якщо задано
   * @param status лише картки в цьому статусі, якщо задано
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка пацієнтів, відсортованих за кличкою
   */
  @GetMapping
  PatientPage listPatients(
      @RequestParam(required = false) @Size(min = 2, max = 100) String q,
      @RequestParam(required = false) UUID ownerId,
      @RequestParam(required = false) PatientStatus status,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return patientService.list(q, ownerId, status, page, pageSize);
  }

  /**
   * Повертає пацієнта за ідентифікатором, зокрема з архівною карткою (операція {@code getPatient},
   * REQ-F-012, REQ-F-016).
   *
   * @param patientId ідентифікатор пацієнта
   * @return пацієнт
   */
  @GetMapping("/{patientId}")
  PatientResponse getPatient(@PathVariable UUID patientId) {
    return patientService.get(patientId);
  }

  /**
   * Переводить картку в статус «Архівна» (операція {@code archivePatient}, REQ-F-015, REQ-F-016);
   * картку не видаляють.
   *
   * @param patientId ідентифікатор пацієнта
   * @return пацієнт зі статусом {@code ARCHIVED}
   */
  @PostMapping("/{patientId}/archive")
  PatientResponse archivePatient(@PathVariable UUID patientId) {
    return patientService.archive(patientId);
  }

  /**
   * Частково оновлює базові дані пацієнта (операція {@code updatePatient}, REQ-F-013, REQ-F-014);
   * власника й статус так змінити не можна.
   *
   * @param patientId ідентифікатор пацієнта
   * @param request передані поля
   * @return оновлений пацієнт
   */
  @PatchMapping("/{patientId}")
  PatientResponse updatePatient(
      @PathVariable UUID patientId, @Valid @RequestBody UpdatePatientRequest request) {
    return patientService.update(patientId, request);
  }
}
