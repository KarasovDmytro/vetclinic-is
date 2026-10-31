package ua.vetclinic.api.common;

/** Недостатньо прав за об'єктом (відповідь 403) із стабільним кодом помилки. */
public class ForbiddenException extends RuntimeException {

	private final String code;

	private final String userMessage;

	public ForbiddenException(String code, String userMessage) {
		super(code + ": " + userMessage);
		this.code = code;
		this.userMessage = userMessage;
	}

	public String code() {
		return code;
	}

	public String userMessage() {
		return userMessage;
	}
}
