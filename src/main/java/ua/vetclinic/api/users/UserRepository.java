package ua.vetclinic.api.users;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface UserRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByEmail(String email);

	Optional<UserEntity> findByEmail(String email);

	@Query("""
			select u from UserEntity u
			where (:role is null or u.role = :role)
			  and (:active is null or u.active = :active)
			""")
	Page<UserEntity> search(@Param("role") Role role, @Param("active") Boolean active, Pageable pageable);
}
