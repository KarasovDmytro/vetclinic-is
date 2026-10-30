package ua.vetclinic.api.users;

class LastAdminException extends RuntimeException {

	LastAdminException() {
		super("Cannot deactivate the last active administrator");
	}
}
