package ua.vetclinic.api.appointments;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Сховище записів на прийом; динамічні фільтри списку будуються через {@code Specification}. */
interface AppointmentRepository
    extends JpaRepository<AppointmentEntity, UUID>, JpaSpecificationExecutor<AppointmentEntity> {

  boolean existsByPatientIdAndStatus(UUID patientId, AppointmentStatus status);

  /**
   * Читає запис із блокуванням рядка до кінця транзакції ({@code SELECT ... FOR UPDATE}), щоб два
   * прийоми для одного запису не збереглися паралельно (AD-07.1).
   *
   * @param id ідентифікатор запису
   * @return заблокований запис або порожньо, якщо його немає
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from AppointmentEntity a where a.id = :id")
  Optional<AppointmentEntity> findByIdForUpdate(@Param("id") UUID id);

  /**
   * Перевіряє, чи зайнятий слот лікаря записом в одному з указаних статусів (конфлікт розкладу,
   * REQ-F-023).
   *
   * @param doctorId лікар
   * @param startsAt початок слота
   * @param statuses статуси, що займають слот
   * @return {@code true}, якщо слот зайнятий
   */
  boolean existsByDoctorIdAndStartsAtAndStatusIn(
      UUID doctorId, OffsetDateTime startsAt, Collection<AppointmentStatus> statuses);

  /**
   * Повертає записи лікаря, що починаються в напіввідкритому інтервалі {@code [from, to)}.
   *
   * @param doctorId лікар
   * @param from початок інтервалу (включно)
   * @param to кінець інтервалу (не включно)
   * @param statuses статуси записів, які потрібно повернути
   * @return записи в довільному порядку
   */
  @Query(
      """
      select a from AppointmentEntity a
      where a.doctorId = :doctorId and a.startsAt >= :from and a.startsAt < :to and a.status in :statuses
      """)
  List<AppointmentEntity> findInRange(
      @Param("doctorId") UUID doctorId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("statuses") Collection<AppointmentStatus> statuses);
}
