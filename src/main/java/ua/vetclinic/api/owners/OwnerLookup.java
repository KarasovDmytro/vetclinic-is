package ua.vetclinic.api.owners;

import java.util.UUID;

import org.springframework.stereotype.Service;

/** Доступ інших модулів до власників без відкриття сховища. */
@Service
public class OwnerLookup {

	private final OwnerRepository ownerRepository;

	OwnerLookup(OwnerRepository ownerRepository) {
		this.ownerRepository = ownerRepository;
	}

	public boolean exists(UUID ownerId) {
		return ownerRepository.existsById(ownerId);
	}
}
