package ua.vetclinic.api.owners;

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
 * REST-ресурс власників {@code /api/v1/owners}.
 *
 * <p>Реєструє й редагує власників адміністратор; лікар має доступ лише на читання.
 */
@RestController
@RequestMapping("/api/v1/owners")
class OwnerController {

  private final OwnerService ownerService;

  OwnerController(OwnerService ownerService) {
    this.ownerService = ownerService;
  }

  /**
   * Реєструє власника (операція {@code createOwner}, REQ-F-010).
   *
   * @param request дані власника
   * @return 201 зі створеним власником і заголовком {@code Location}
   */
  @PostMapping
  ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody CreateOwnerRequest request) {
    OwnerResponse body = ownerService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/owners/" + body.id())).body(body);
  }

  /**
   * Шукає власників за ПІБ (операція {@code listOwners}, REQ-F-017, REQ-NF-003).
   *
   * @param q підрядок ПІБ без урахування регістру, від 2 до 100 символів
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка власників, відсортованих за ПІБ
   */
  @GetMapping
  OwnerPage listOwners(
      @RequestParam(required = false) @Size(min = 2, max = 100) String q,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return ownerService.list(q, page, pageSize);
  }

  /**
   * Повертає власника за ідентифікатором (операція {@code getOwner}, REQ-F-010).
   *
   * @param ownerId ідентифікатор власника
   * @return власник
   */
  @GetMapping("/{ownerId}")
  OwnerResponse getOwner(@PathVariable UUID ownerId) {
    return ownerService.get(ownerId);
  }

  /**
   * Частково оновлює дані власника (операція {@code updateOwner}, REQ-F-011).
   *
   * @param ownerId ідентифікатор власника
   * @param request передані поля; явний {@code null} очищає необов'язкове поле
   * @return оновлений власник
   */
  @PatchMapping("/{ownerId}")
  OwnerResponse updateOwner(
      @PathVariable UUID ownerId, @Valid @RequestBody UpdateOwnerRequest request) {
    return ownerService.update(ownerId, request);
  }
}
