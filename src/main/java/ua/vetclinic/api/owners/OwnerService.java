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

@Service
class OwnerService {

	private final OwnerRepository ownerRepository;

	private final AuditLog auditLog;

	OwnerService(OwnerRepository ownerRepository, AuditLog auditLog) {
		this.ownerRepository = ownerRepository;
		this.auditLog = auditLog;
	}

	OwnerResponse create(CreateOwnerRequest request) {
		OffsetDateTime now = OffsetDateTime.now();
		OwnerEntity owner = new OwnerEntity(UUID.randomUUID(), request.fullName(), request.phone(), request.email(),
				request.address(), now, now);
		OwnerResponse created = ownerRepository.save(owner).toResponse();
		auditLog.success("OWNER_CREATED", "OWNER", created.id());
		return created;
	}

	OwnerResponse get(UUID ownerId) {
		return findOrThrow(ownerId).toResponse();
	}

	OwnerResponse update(UUID ownerId, UpdateOwnerRequest request) {
		if (request.isEmpty()) {
			throw new EmptyBodyException();
		}
		List<ErrorDetail> missingRequired = request.nullRequiredFields();
		if (!missingRequired.isEmpty()) {
			throw new RequestValidationException(missingRequired);
		}
		OwnerEntity owner = findOrThrow(ownerId);
		owner.apply(request, OffsetDateTime.now());
		OwnerResponse updated = ownerRepository.save(owner).toResponse();
		auditLog.success("OWNER_UPDATED", "OWNER", ownerId);
		return updated;
	}

	OwnerPage list(String query, int page, int pageSize) {
		Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("fullName"));
		Page<OwnerEntity> owners = query == null ? ownerRepository.findAll(pageable)
				: ownerRepository.findByFullNameContainingIgnoreCase(query, pageable);
		return new OwnerPage(page, pageSize, owners.getTotalElements(),
				owners.getContent().stream().map(OwnerEntity::toResponse).toList());
	}

	private OwnerEntity findOrThrow(UUID ownerId) {
		return ownerRepository.findById(ownerId).orElseThrow(() -> new OwnerNotFoundException(ownerId));
	}
}
