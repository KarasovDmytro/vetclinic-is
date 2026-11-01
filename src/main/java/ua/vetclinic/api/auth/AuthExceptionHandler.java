package ua.vetclinic.api.auth;

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
class AuthExceptionHandler {

	@ExceptionHandler(InvalidCredentialsException.class)
	ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception,
			HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Невірний email або пароль",
				request);
	}

	@ExceptionHandler(AccountDeactivatedException.class)
	ResponseEntity<ErrorResponse> handleAccountDeactivated(AccountDeactivatedException exception,
			HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.FORBIDDEN, "ACCOUNT_DEACTIVATED", "Обліковий запис деактивовано",
				request);
	}
}
