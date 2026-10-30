package ua.vetclinic.api.owners;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;

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
		return ownerRepository.findById(ownerId).orElseThrow(() -> new OwnerNotFoundException(ownerId)).toResponse();
	}
}
