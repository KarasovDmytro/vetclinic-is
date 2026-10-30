package ua.vetclinic.api.common;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ApiExceptionHandler {

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception,
			HttpServletRequest request) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).headers(exception.getHeaders())
				.body(ErrorResponse.of(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED",
						"Метод не підтримується", request));
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
			HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_MEDIA_TYPE",
				"Підтримується лише application/json", request);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception,
			HttpServletRequest request) {
		if (exception.getCause() instanceof InvalidFormatException cause && cause.getTargetType().isEnum()
				&& !cause.getPath().isEmpty()) {
			return validationError(request, List.of(new ErrorDetail(cause.getPath().getLast().getPropertyName(),
					"INVALID_ENUM", "Недопустиме значення")));
		}
		return malformedRequest(request);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception,
			HttpServletRequest request) {
		return malformedRequest(request);
	}

	private static ResponseEntity<ErrorResponse> malformedRequest(HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST", "Запит не вдалося розібрати",
				request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		return validationError(request, exception.getBindingResult().getFieldErrors().stream()
				.map(error -> new ErrorDetail(error.getField(), reasonOf(error), error.getDefaultMessage()))
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

	private static String reasonOf(FieldError error) {
		if ("Size".equals(error.getCode())) {
			return isBelowMinimum(error) ? "TOO_SHORT" : "TOO_LONG";
		}
		return reasonOf(error.getCode());
	}

	private static boolean isBelowMinimum(FieldError error) {
		ConstraintViolation<?> violation = error.unwrap(ConstraintViolation.class);
		int min = (int) violation.getConstraintDescriptor().getAttributes().get("min");
		return violation.getInvalidValue() instanceof CharSequence value && value.length() < min;
	}

	private static String reasonOf(String constraintCode) {
		return switch (constraintCode == null ? "" : constraintCode) {
			case "NotBlank", "NotNull" -> "REQUIRED";
			case "Min", "Max" -> "OUT_OF_RANGE";
			default -> "INVALID_FORMAT";
		};
	}
}
