package ua.vetclinic.api.users;

import java.util.UUID;

class UserAlreadyDeactivatedException extends RuntimeException {

	UserAlreadyDeactivatedException(UUID userId) {
		super("User already deactivated: " + userId);
	}
}
