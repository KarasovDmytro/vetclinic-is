package ua.vetclinic.api.common;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(exception.getHeaders()).body(ErrorResponse.of(
				HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "Метод не підтримується", request));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ErrorResponse.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "Дані запиту некоректні", request));
	}
}
