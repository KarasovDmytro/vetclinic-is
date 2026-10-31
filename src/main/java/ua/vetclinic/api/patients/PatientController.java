package ua.vetclinic.api.patients;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("/{patientId}")
	PatientResponse getPatient(@PathVariable UUID patientId) {
		return patientService.get(patientId);
	}

	@PatchMapping("/{patientId}")
	PatientResponse updatePatient(@PathVariable UUID patientId, @Valid @RequestBody UpdatePatientRequest request) {
		return patientService.update(patientId, request);
	}
}
