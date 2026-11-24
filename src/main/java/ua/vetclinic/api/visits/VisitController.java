package ua.vetclinic.api.visits;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-ресурс прийомів: створення за записом, редагування та медична картка пацієнта.
 *
 * <p>Усі операції доступні лише лікарю; лікар визначається з claim {@code sub} токена.
 */
@RestController
@RequestMapping("/api/v1")
class VisitController {

  private final VisitService visitService;

  VisitController(VisitService visitService) {
    this.visitService = visitService;
  }

  /**
   * Зберігає результати прийому за записом і списує витрачені препарати (операція {@code
   * createVisit}, REQ-F-027, REQ-F-031–033, REQ-F-044–048).
   *
   * @param jwt токен поточного лікаря
   * @param appointmentId запис, за яким проведено прийом
   * @param request діагноз, призначення, нотатки й препарати
   * @return 201 зі створеним прийомом і заголовком {@code Location}
   */
  @PostMapping("/appointments/{appointmentId}/visit")
  ResponseEntity<VisitResponse> createVisit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID appointmentId,
      @Valid @RequestBody CreateVisitRequest request) {
    VisitResponse body =
        visitService.create(UUID.fromString(jwt.getSubject()), appointmentId, request);
    return ResponseEntity.created(URI.create("/api/v1/visits/" + body.id())).body(body);
  }

  /**
   * Повертає прийом за ідентифікатором (операція {@code getVisit}, REQ-F-030).
   *
   * @param visitId ідентифікатор прийому
   * @return прийом
   */
  @GetMapping("/visits/{visitId}")
  VisitResponse getVisit(@PathVariable UUID visitId) {
    return visitService.get(visitId);
  }

  /**
   * Редагує текстові результати власного прийому (операція {@code updateVisit}, REQ-F-031,
   * REQ-F-032, REQ-F-035).
   *
   * @param jwt токен поточного лікаря
   * @param visitId ідентифікатор прийому
   * @param request передані поля
   * @return оновлений прийом
   */
  @PatchMapping("/visits/{visitId}")
  VisitResponse updateVisit(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID visitId,
      @Valid @RequestBody UpdateVisitRequest request) {
    return visitService.update(UUID.fromString(jwt.getSubject()), visitId, request);
  }

  /**
   * Повертає хронологію прийомів пацієнта — його медичну картку (операція {@code
   * listPatientVisits}, REQ-F-030, REQ-F-034).
   *
   * @param patientId ідентифікатор пацієнта, зокрема з архівною карткою
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка прийомів, новіші спочатку
   */
  @GetMapping("/patients/{patientId}/visits")
  VisitPage listPatientVisits(
      @PathVariable UUID patientId,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return visitService.listByPatient(patientId, page, pageSize);
  }
}
