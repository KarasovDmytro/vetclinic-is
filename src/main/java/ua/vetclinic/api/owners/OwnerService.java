package ua.vetclinic.api.owners;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.common.EmptyBodyException;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.RequestValidationException;
import ua.vetclinic.api.common.ResourceNotFoundException;

/**
 * Бізнес-логіка власників: реєстрація, редагування, пошук і журнал аудиту змін без персональних
 * даних.
 */
@Service
class OwnerService {

  private final OwnerRepository ownerRepository;

  private final AuditLog auditLog;

  OwnerService(OwnerRepository ownerRepository, AuditLog auditLog) {
    this.ownerRepository = ownerRepository;
    this.auditLog = auditLog;
  }

  /**
   * Реєструє власника.
   *
   * @param request дані власника
   * @return створений власник
   */
  OwnerResponse create(CreateOwnerRequest request) {
    OffsetDateTime now = OffsetDateTime.now();
    OwnerEntity owner =
        new OwnerEntity(
            UUID.randomUUID(),
            request.fullName(),
            request.phone(),
            request.email(),
            request.address(),
            now,
            now);
    OwnerResponse created = ownerRepository.save(owner).toResponse();
    auditLog.success("OWNER_CREATED", "OWNER", created.id());
    return created;
  }

  /**
   * Повертає власника за ідентифікатором.
   *
   * @param ownerId ідентифікатор власника
   * @return власник
   * @throws ResourceNotFoundException якщо власника немає
   */
  OwnerResponse get(UUID ownerId) {
    return findOrThrow(ownerId).toResponse();
  }

  /**
   * Частково оновлює дані власника.
   *
   * @param ownerId ідентифікатор власника
   * @param request передані поля
   * @return оновлений власник
   * @throws EmptyBodyException якщо тіло не містить жодного поля
   * @throws RequestValidationException якщо {@code fullName} або {@code phone} передано як {@code
   *     null}
   * @throws ResourceNotFoundException якщо власника немає
   */
  OwnerResponse update(UUID ownerId, UpdateOwnerRequest request) {
    if (request.isEmpty()) {
      auditLog.failure("OWNER_UPDATED", "OWNER", ownerId, "VALIDATION_ERROR");
      throw new EmptyBodyException();
    }
    List<ErrorDetail> missingRequired = request.nullRequiredFields();
    if (!missingRequired.isEmpty()) {
      auditLog.failure("OWNER_UPDATED", "OWNER", ownerId, "VALIDATION_ERROR");
      throw new RequestValidationException(missingRequired);
    }
    OwnerEntity owner = findOrThrow(ownerId);
    owner.apply(request, OffsetDateTime.now());
    OwnerResponse updated = ownerRepository.save(owner).toResponse();
    auditLog.success("OWNER_UPDATED", "OWNER", ownerId);
    return updated;
  }

  /**
   * Повертає сторінку власників, за потреби відфільтровану за підрядком ПІБ.
   *
   * @param query підрядок ПІБ або {@code null}
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки
   * @return сторінка власників, відсортованих за ПІБ
   */
  OwnerPage list(String query, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("fullName", "id"));
    Page<OwnerEntity> owners =
        query == null
            ? ownerRepository.findAll(pageable)
            : ownerRepository.findByFullNameContainingIgnoreCase(query, pageable);
    return new OwnerPage(
        page,
        pageSize,
        owners.getTotalElements(),
        owners.getContent().stream().map(OwnerEntity::toResponse).toList());
  }

  private OwnerEntity findOrThrow(UUID ownerId) {
    return ownerRepository
        .findById(ownerId)
        .orElseThrow(() -> new ResourceNotFoundException("Owner not found: " + ownerId));
  }
}
