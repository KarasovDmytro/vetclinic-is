package ua.vetclinic.api.common;

import java.util.List;

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
		List<ErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorDetail(error.getField(), reasonOf(error.getCode()), error.getDefaultMessage()))
				.toList();
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ErrorResponse.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "Дані запиту некоректні", request, details));
	}

	private static String reasonOf(String constraintCode) {
		return switch (constraintCode == null ? "" : constraintCode) {
			case "NotBlank", "NotNull" -> "REQUIRED";
			default -> "INVALID_FORMAT";
		};
	}
}
