package ua.vetclinic.api.users;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class UserExceptionHandler {

	@ExceptionHandler(EmailTakenException.class)
	ResponseEntity<Map<String, String>> handleEmailTaken(EmailTakenException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "EMAIL_TAKEN"));
	}

	@ExceptionHandler(UserNotFoundException.class)
	ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("code", "NOT_FOUND"));
	}

	@ExceptionHandler(UserAlreadyDeactivatedException.class)
	ResponseEntity<Map<String, String>> handleUserAlreadyDeactivated(UserAlreadyDeactivatedException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("code", "USER_ALREADY_DEACTIVATED"));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<Map<String, String>> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(Map.of("code", "METHOD_NOT_ALLOWED"));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("code", "VALIDATION_ERROR"));
	}
}
