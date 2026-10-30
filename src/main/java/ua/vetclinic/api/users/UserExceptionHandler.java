package ua.vetclinic.api.users;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(Map.of("code", "VALIDATION_ERROR"));
	}
}
