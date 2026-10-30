package ua.vetclinic.api.users;

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
class UserExceptionHandler {

	@ExceptionHandler(EmailTakenException.class)
	ResponseEntity<ErrorResponse> handleEmailTaken(EmailTakenException exception, HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.CONFLICT, "EMAIL_TAKEN", "Email вже зайнятий", request);
	}

	@ExceptionHandler(UserNotFoundException.class)
	ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException exception, HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ресурс не знайдено", request);
	}

	@ExceptionHandler(UserAlreadyDeactivatedException.class)
	ResponseEntity<ErrorResponse> handleUserAlreadyDeactivated(UserAlreadyDeactivatedException exception,
			HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.CONFLICT, "USER_ALREADY_DEACTIVATED",
				"Користувача вже деактивовано", request);
	}
}
