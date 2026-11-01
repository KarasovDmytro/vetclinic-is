package ua.vetclinic.api.common;

/** Об'єкта, указаного в шляху запиту, не існує (відповідь 404 NOT_FOUND). */
public class ResourceNotFoundException extends RuntimeException {

	public ResourceNotFoundException(String message) {
		super(message);
	}
}
