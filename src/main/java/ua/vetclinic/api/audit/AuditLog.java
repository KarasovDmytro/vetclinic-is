package ua.vetclinic.api.audit;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/** Журнал аудиту (REQ-NF-006, AD-22): одна структурована подія в рядок, іменований логер AUDIT. */
@Component
public class AuditLog {

	private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

	public void success(String event, UUID actorId, String actorRole, String entityType, UUID entityId) {
		AUDIT.info("event={} actorId={} actorRole={} entityType={} entityId={} outcome=SUCCESS requestId={}", event,
				actorId, actorRole, entityType, entityId, MDC.get("requestId"));
	}

	public void failure(String event, UUID actorId, String actorRole, String entityType, UUID entityId,
			String errorCode) {
		AUDIT.info("event={} actorId={} actorRole={} entityType={} entityId={} outcome=FAILURE requestId={} errorCode={}",
				event, actorId, actorRole, entityType, entityId, MDC.get("requestId"), errorCode);
	}
}
