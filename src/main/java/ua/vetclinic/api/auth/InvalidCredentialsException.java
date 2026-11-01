package ua.vetclinic.api.auth;

class InvalidCredentialsException extends RuntimeException {

	InvalidCredentialsException() {
		super("Invalid credentials");
	}
}
