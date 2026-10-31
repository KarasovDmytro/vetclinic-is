package ua.vetclinic.api.visits;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
class VisitController {

	private final VisitService visitService;

	VisitController(VisitService visitService) {
		this.visitService = visitService;
	}

	@PostMapping("/appointments/{appointmentId}/visit")
	ResponseEntity<VisitResponse> createVisit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID appointmentId,
			@Valid @RequestBody CreateVisitRequest request) {
		VisitResponse body = visitService.create(UUID.fromString(jwt.getSubject()), appointmentId, request);
		return ResponseEntity.created(URI.create("/api/v1/visits/" + body.id())).body(body);
	}

	@GetMapping("/visits/{visitId}")
	VisitResponse getVisit(@PathVariable UUID visitId) {
		return visitService.get(visitId);
	}

	@PatchMapping("/visits/{visitId}")
	VisitResponse updateVisit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID visitId,
			@Valid @RequestBody UpdateVisitRequest request) {
		return visitService.update(UUID.fromString(jwt.getSubject()), visitId, request);
	}

	@GetMapping("/patients/{patientId}/visits")
	VisitPage listPatientVisits(@PathVariable UUID patientId, @RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return visitService.listByPatient(patientId, page, pageSize);
	}
}
