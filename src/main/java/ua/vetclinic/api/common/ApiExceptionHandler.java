package ua.vetclinic.api.common;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(exception.getHeaders())
				.body(ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
						"Метод не підтримується", request));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		return validationError(request, exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorDetail(error.getField(), reasonOf(error.getCode()), error.getDefaultMessage()))
				.toList());
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException exception,
			HttpServletRequest request) {
		return validationError(request, exception.getParameterValidationResults().stream()
				.flatMap(result -> result.getResolvableErrors().stream()
						.map(error -> new ErrorDetail(result.getMethodParameter().getParameterName(),
								reasonOf(constraintOf(error)), error.getDefaultMessage())))
				.toList());
	}

	private static ResponseEntity<ErrorResponse> validationError(HttpServletRequest request,
			List<ErrorDetail> details) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(ErrorResponse.of(
				HttpStatus.UNPROCESSABLE_CONTENT, "VALIDATION_ERROR", "Дані запиту некоректні", request, details));
	}

	private static String constraintOf(MessageSourceResolvable error) {
		String[] codes = error.getCodes();
		return codes == null || codes.length == 0 ? null : codes[codes.length - 1];
	}

	private static String reasonOf(String constraintCode) {
		return switch (constraintCode == null ? "" : constraintCode) {
			case "NotBlank", "NotNull" -> "REQUIRED";
			case "Min", "Max" -> "OUT_OF_RANGE";
			default -> "INVALID_FORMAT";
		};
	}
}
