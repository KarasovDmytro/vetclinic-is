package ua.vetclinic.api.users;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ua.vetclinic.api.common.ErrorResponse;

@RestControllerAdvice
class UserExceptionHandler {

	@ExceptionHandler(EmailTakenException.class)
	ResponseEntity<ErrorResponse> handleEmailTaken(EmailTakenException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("EMAIL_TAKEN"));
	}

	@ExceptionHandler(UserNotFoundException.class)
	ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("NOT_FOUND"));
	}

	@ExceptionHandler(UserAlreadyDeactivatedException.class)
	ResponseEntity<ErrorResponse> handleUserAlreadyDeactivated(UserAlreadyDeactivatedException exception) {
		return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("USER_ALREADY_DEACTIVATED"));
	}
}
