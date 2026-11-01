package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AppointmentRepository
		extends JpaRepository<AppointmentEntity, UUID>, JpaSpecificationExecutor<AppointmentEntity> {

	boolean existsByPatientIdAndStatus(UUID patientId, AppointmentStatus status);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from AppointmentEntity a where a.id = :id")
	Optional<AppointmentEntity> findByIdForUpdate(@Param("id") UUID id);

	boolean existsByDoctorIdAndStartsAtAndStatusIn(UUID doctorId, OffsetDateTime startsAt,
			Collection<AppointmentStatus> statuses);

	@Query("""
			select a from AppointmentEntity a
			where a.doctorId = :doctorId and a.startsAt >= :from and a.startsAt < :to and a.status in :statuses
			""")
	List<AppointmentEntity> findInRange(@Param("doctorId") UUID doctorId, @Param("from") OffsetDateTime from,
			@Param("to") OffsetDateTime to, @Param("statuses") Collection<AppointmentStatus> statuses);
}
