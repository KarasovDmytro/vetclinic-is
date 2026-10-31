package ua.vetclinic.api.common;

import java.util.List;

/** Конфлікт зі станом даних (відповідь 409) із стабільним кодом помилки та необов'язковими деталями. */
public class ConflictException extends RuntimeException {

	private final String code;

	private final String userMessage;

	private final List<ErrorDetail> details;

	public ConflictException(String code, String userMessage) {
		this(code, userMessage, null);
	}

	public ConflictException(String code, String userMessage, List<ErrorDetail> details) {
		super(code + ": " + userMessage);
		this.code = code;
		this.userMessage = userMessage;
		this.details = details;
	}

	public String code() {
		return code;
	}

	public String userMessage() {
		return userMessage;
	}

	public List<ErrorDetail> details() {
		return details;
	}
}
