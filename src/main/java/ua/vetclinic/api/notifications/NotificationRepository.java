package ua.vetclinic.api.notifications;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {

	boolean existsByMedicationIdAndReadFalse(UUID medicationId);

	Page<NotificationEntity> findByRead(boolean read, Pageable pageable);
}
