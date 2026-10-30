package ua.vetclinic.api.common;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
class UnexpectedExceptionHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(UnexpectedExceptionHandler.class);

	@ExceptionHandler(RuntimeException.class)
	ResponseEntity<ErrorResponse> handleUnexpected(RuntimeException exception, HttpServletRequest request) {
		ResponseEntity<ErrorResponse> response = ErrorResponse.entity(HttpStatus.INTERNAL_SERVER_ERROR,
				"INTERNAL_ERROR", "Внутрішня помилка сервера", request);
		LOGGER.error("Unhandled exception, traceId={}", response.getBody().traceId(), exception);
		return response;
	}
}
