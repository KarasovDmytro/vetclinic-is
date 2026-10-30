package ua.vetclinic.api.common;

import java.util.List;

/** Помилки полів, виявлені вже в бізнес-логіці (після розбору тіла), відповідь 422 VALIDATION_ERROR. */
public class RequestValidationException extends RuntimeException {

	private final List<ErrorDetail> details;

	public RequestValidationException(List<ErrorDetail> details) {
		super("Request fields are invalid: " + details);
		this.details = details;
	}

	public List<ErrorDetail> details() {
		return details;
	}
}
