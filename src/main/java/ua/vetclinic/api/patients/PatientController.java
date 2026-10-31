package ua.vetclinic.api.patients;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

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
@RequestMapping("/api/v1/patients")
class PatientController {

	private final PatientService patientService;

	PatientController(PatientService patientService) {
		this.patientService = patientService;
	}

	@PostMapping
	ResponseEntity<PatientResponse> createPatient(@Valid @RequestBody CreatePatientRequest request) {
		PatientResponse body = patientService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/patients/" + body.id())).body(body);
	}

	@GetMapping
	PatientPage listPatients(@RequestParam(required = false) @Size(min = 2, max = 100) String q,
			@RequestParam(required = false) UUID ownerId, @RequestParam(required = false) PatientStatus status,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return patientService.list(q, ownerId, status, page, pageSize);
	}

	@GetMapping("/{patientId}")
	PatientResponse getPatient(@PathVariable UUID patientId) {
		return patientService.get(patientId);
	}

	@PostMapping("/{patientId}/archive")
	PatientResponse archivePatient(@PathVariable UUID patientId) {
		return patientService.archive(patientId);
	}

	@PatchMapping("/{patientId}")
	PatientResponse updatePatient(@PathVariable UUID patientId, @Valid @RequestBody UpdatePatientRequest request) {
		return patientService.update(patientId, request);
	}
}
