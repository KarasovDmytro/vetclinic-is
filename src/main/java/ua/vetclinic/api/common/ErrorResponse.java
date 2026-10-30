package ua.vetclinic.api.common;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;

public record ErrorResponse(String code, String message, int status, OffsetDateTime timestamp, String path,
		String traceId, @JsonInclude(JsonInclude.Include.NON_NULL) List<ErrorDetail> details) {

	private static final ZoneId CLINIC_ZONE = ZoneId.of("Europe/Kyiv");

	public static ErrorResponse of(HttpStatus status, String code, String message, HttpServletRequest request) {
		return of(status, code, message, request, null);
	}

	public static ErrorResponse of(HttpStatus status, String code, String message, HttpServletRequest request,
			List<ErrorDetail> details) {
		return new ErrorResponse(code, message, status.value(), OffsetDateTime.now(CLINIC_ZONE),
				request.getRequestURI(), (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE),
				details);
	}
}
