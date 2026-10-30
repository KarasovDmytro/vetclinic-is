package ua.vetclinic.api.owners;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface OwnerRepository extends JpaRepository<OwnerEntity, UUID> {
}
