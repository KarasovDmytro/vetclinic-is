package ua.vetclinic.api.common;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;

public record ErrorResponse(String code, String message, int status, OffsetDateTime timestamp, String path,
		String traceId) {

	private static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Kyiv");

	public static ErrorResponse of(HttpStatus status, String code, String message, HttpServletRequest request) {
		return new ErrorResponse(code, message, status.value(), OffsetDateTime.now(CLINIC_ZONE),
				request.getRequestURI(), UUID.randomUUID().toString());
	}
}
