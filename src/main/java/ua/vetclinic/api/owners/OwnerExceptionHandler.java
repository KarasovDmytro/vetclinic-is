package ua.vetclinic.api.owners;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.vetclinic.api.common.ErrorResponse;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class OwnerExceptionHandler {

	@ExceptionHandler(OwnerNotFoundException.class)
	ResponseEntity<ErrorResponse> handleOwnerNotFound(OwnerNotFoundException exception, HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ресурс не знайдено", request);
	}
}
