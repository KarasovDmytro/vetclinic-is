package ua.vetclinic.api.owners;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface OwnerRepository extends JpaRepository<OwnerEntity, UUID> {

	Page<OwnerEntity> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
}
