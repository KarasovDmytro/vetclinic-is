package ua.vetclinic.api.owners;

import java.util.UUID;

class OwnerNotFoundException extends RuntimeException {

	OwnerNotFoundException(UUID ownerId) {
		super("Owner not found: " + ownerId);
	}
}
