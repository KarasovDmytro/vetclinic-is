package ua.vetclinic.api.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Сховище облікових записів; email зберігається в нижньому регістрі. */
interface UserRepository extends JpaRepository<UserEntity, UUID> {

  boolean existsByEmail(String email);

  Optional<UserEntity> findByEmail(String email);

  long countByRoleAndActive(Role role, boolean active);

  List<UserEntity> findByRoleAndActiveTrueOrderByFullName(Role role);

  /**
   * Повертає сторінку користувачів; {@code null} у фільтрі означає «без обмеження».
   *
   * @param role роль або {@code null}
   * @param active ознака активності або {@code null}
   * @param pageable сторінка й сортування
   * @return сторінка користувачів
   */
  @Query(
      """
      select u from UserEntity u
      where (:role is null or u.role = :role)
        and (:active is null or u.active = :active)
      """)
  Page<UserEntity> search(
      @Param("role") Role role, @Param("active") Boolean active, Pageable pageable);
}
