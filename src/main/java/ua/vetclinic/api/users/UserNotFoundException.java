package ua.vetclinic.api.users;

import java.util.UUID;

class UserNotFoundException extends RuntimeException {

	UserNotFoundException(UUID userId) {
		super("User not found: " + userId);
	}
}
