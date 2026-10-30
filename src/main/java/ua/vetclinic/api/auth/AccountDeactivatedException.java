package ua.vetclinic.api.auth;

class AccountDeactivatedException extends RuntimeException {

	AccountDeactivatedException() {
		super("Account deactivated");
	}
}
