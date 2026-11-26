package ua.vetclinic.api.medications;

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
 * REST-ресурс каталогу медикаментів {@code /api/v1/medications}.
 *
 * <p>Каталог веде лише адміністратор; лікар може переглядати його, щоб обрати препарати для
 * прийому.
 */
@RestController
@RequestMapping("/api/v1/medications")
class MedicationController {

  private final MedicationService medicationService;

  MedicationController(MedicationService medicationService) {
    this.medicationService = medicationService;
  }

  /**
   * Додає медикамент до каталогу (операція {@code createMedication}, REQ-F-040, REQ-F-043).
   *
   * @param request назва, початковий залишок і поріг
   * @return 201 зі створеним медикаментом і заголовком {@code Location}
   */
  @PostMapping
  ResponseEntity<MedicationResponse> createMedication(
      @Valid @RequestBody CreateMedicationRequest request) {
    MedicationResponse body = medicationService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/medications/" + body.id())).body(body);
  }

  /**
   * Повертає сторінку каталогу (операція {@code listMedications}, REQ-F-044).
   *
   * @param q підрядок назви без урахування регістру, від 2 до 100 символів
   * @param lowStock {@code true} — лише позиції із залишком нижче порогу, {@code false} — решта
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка медикаментів, відсортованих за назвою
   */
  @GetMapping
  MedicationPage listMedications(
      @RequestParam(required = false) @Size(min = 2, max = 100) String q,
      @RequestParam(required = false) Boolean lowStock,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return medicationService.list(q, lowStock, page, pageSize);
  }

  /**
   * Повертає медикамент за ідентифікатором (операція {@code getMedication}, REQ-F-040).
   *
   * @param medicationId ідентифікатор медикаменту
   * @return медикамент
   */
  @GetMapping("/{medicationId}")
  MedicationResponse getMedication(@PathVariable UUID medicationId) {
    return medicationService.get(medicationId);
  }

  /**
   * Змінює назву та/або поріг медикаменту (операція {@code updateMedication}, REQ-F-041,
   * REQ-F-043). Залишок цим запитом не змінюється.
   *
   * @param medicationId ідентифікатор медикаменту
   * @param request поля для зміни
   * @return оновлений медикамент
   */
  @PatchMapping("/{medicationId}")
  MedicationResponse updateMedication(
      @PathVariable UUID medicationId, @Valid @RequestBody UpdateMedicationRequest request) {
    return medicationService.update(medicationId, request);
  }

  /**
   * Коригує залишок на задану зміну, наприклад після надходження чи інвентаризації (операція {@code
   * adjustMedicationStock}, REQ-F-042).
   *
   * @param medicationId ідентифікатор медикаменту
   * @param request зміна залишку
   * @return медикамент з новим залишком
   */
  @PostMapping("/{medicationId}/adjust-stock")
  MedicationResponse adjustMedicationStock(
      @PathVariable UUID medicationId, @Valid @RequestBody StockAdjustmentRequest request) {
    return medicationService.adjustStock(medicationId, request);
  }
}
