package ua.vetclinic.api.users;

class EmailTakenException extends RuntimeException {

	EmailTakenException(String email) {
		super("Email already taken: " + email);
	}
}
