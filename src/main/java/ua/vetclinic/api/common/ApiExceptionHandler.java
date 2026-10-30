package ua.vetclinic.api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(new ErrorResponse("METHOD_NOT_ALLOWED"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErrorResponse("VALIDATION_ERROR"));
	}
}
