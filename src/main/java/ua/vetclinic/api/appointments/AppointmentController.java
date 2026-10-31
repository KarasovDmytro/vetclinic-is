package ua.vetclinic.api.appointments;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
class AppointmentController {

	private final AppointmentService appointmentService;

	AppointmentController(AppointmentService appointmentService) {
		this.appointmentService = appointmentService;
	}

	@PostMapping
	ResponseEntity<AppointmentResponse> createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
		AppointmentResponse body = appointmentService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/appointments/" + body.id())).body(body);
	}

	@GetMapping
	AppointmentPage listAppointments(@RequestParam(required = false) UUID doctorId,
			@RequestParam(required = false) UUID patientId, @RequestParam(required = false) AppointmentStatus status,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return appointmentService.list(doctorId, patientId, status, from, to, page, pageSize);
	}

	@GetMapping("/{appointmentId}")
	AppointmentResponse getAppointment(@PathVariable UUID appointmentId) {
		return appointmentService.get(appointmentId);
	}

	@PatchMapping("/{appointmentId}")
	AppointmentResponse rescheduleAppointment(@PathVariable UUID appointmentId,
			@Valid @RequestBody UpdateAppointmentRequest request) {
		return appointmentService.reschedule(appointmentId, request);
	}

	@PostMapping("/{appointmentId}/cancel")
	AppointmentResponse cancelAppointment(@PathVariable UUID appointmentId) {
		return appointmentService.cancel(appointmentId);
	}
}
