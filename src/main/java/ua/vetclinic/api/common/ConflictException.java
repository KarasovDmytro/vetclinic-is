package ua.vetclinic.api.common;

/** Конфлікт зі станом даних (відповідь 409) із стабільним кодом помилки. */
public class ConflictException extends RuntimeException {

	private final String code;

	private final String userMessage;

	public ConflictException(String code, String userMessage) {
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
