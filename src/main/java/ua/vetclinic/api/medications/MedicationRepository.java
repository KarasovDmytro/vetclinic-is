package ua.vetclinic.api.medications;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface MedicationRepository extends JpaRepository<MedicationEntity, UUID>, JpaSpecificationExecutor<MedicationEntity> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select m from MedicationEntity m where m.id = :id")
	Optional<MedicationEntity> findByIdForUpdate(@Param("id") UUID id);
}
