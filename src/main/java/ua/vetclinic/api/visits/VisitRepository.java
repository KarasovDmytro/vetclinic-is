package ua.vetclinic.api.visits;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface VisitRepository extends JpaRepository<VisitEntity, UUID> {

	Page<VisitEntity> findByPatientId(UUID patientId, Pageable pageable);
}
