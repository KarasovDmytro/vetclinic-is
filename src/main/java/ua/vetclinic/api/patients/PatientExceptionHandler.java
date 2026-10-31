package ua.vetclinic.api.patients;

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
class PatientExceptionHandler {

	@ExceptionHandler(PatientNotFoundException.class)
	ResponseEntity<ErrorResponse> handlePatientNotFound(PatientNotFoundException exception,
			HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.NOT_FOUND, "NOT_FOUND", "Ресурс не знайдено", request);
	}

	@ExceptionHandler(PatientArchivedException.class)
	ResponseEntity<ErrorResponse> handlePatientArchived(PatientArchivedException exception,
			HttpServletRequest request) {
		return ErrorResponse.entity(HttpStatus.CONFLICT, "PATIENT_ARCHIVED", "Картку пацієнта архівовано", request);
	}
}
