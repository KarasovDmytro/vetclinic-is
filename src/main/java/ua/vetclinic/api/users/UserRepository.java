package ua.vetclinic.api.users;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface UserRepository extends JpaRepository<UserEntity, UUID> {

	boolean existsByEmail(String email);
}
