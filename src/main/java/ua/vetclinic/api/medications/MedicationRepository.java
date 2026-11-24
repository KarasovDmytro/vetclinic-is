package ua.vetclinic.api.medications;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Сховище каталогу медикаментів; блокувальні запити використовуються при зміні залишку. */
interface MedicationRepository
    extends JpaRepository<MedicationEntity, UUID>, JpaSpecificationExecutor<MedicationEntity> {

  boolean existsByNameIgnoreCase(String name);

  boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

  /**
   * Читає медикамент із блокуванням рядка для ручного коригування залишку.
   *
   * @param id ідентифікатор медикаменту
   * @return заблокований медикамент або порожньо
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from MedicationEntity m where m.id = :id")
  Optional<MedicationEntity> findByIdForUpdate(@Param("id") UUID id);

  /**
   * Повертає ті з переданих ідентифікаторів, що є в каталозі, без завантаження сутностей.
   *
   * @param ids ідентифікатори для перевірки
   * @return наявні ідентифікатори
   */
  @Query("select m.id from MedicationEntity m where m.id in :ids")
  List<UUID> findExistingIds(@Param("ids") Collection<UUID> ids);

  /**
   * Читає медикаменти з блокуванням рядків у порядку ідентифікаторів, щоб паралельні списання не
   * потрапили у взаємне блокування (REQ-F-048).
   *
   * @param ids ідентифікатори медикаментів
   * @return заблоковані медикаменти
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select m from MedicationEntity m where m.id in :ids order by m.id")
  List<MedicationEntity> findAllForUpdate(@Param("ids") Collection<UUID> ids);
}
