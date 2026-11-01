package ua.vetclinic.api.medications;

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
@RequestMapping("/api/v1/medications")
class MedicationController {

	private final MedicationService medicationService;

	MedicationController(MedicationService medicationService) {
		this.medicationService = medicationService;
	}

	@PostMapping
	ResponseEntity<MedicationResponse> createMedication(@Valid @RequestBody CreateMedicationRequest request) {
		MedicationResponse body = medicationService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/medications/" + body.id())).body(body);
	}

	@GetMapping
	MedicationPage listMedications(@RequestParam(required = false) @Size(min = 2, max = 100) String q,
			@RequestParam(required = false) Boolean lowStock, @RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return medicationService.list(q, lowStock, page, pageSize);
	}

	@GetMapping("/{medicationId}")
	MedicationResponse getMedication(@PathVariable UUID medicationId) {
		return medicationService.get(medicationId);
	}

	@PatchMapping("/{medicationId}")
	MedicationResponse updateMedication(@PathVariable UUID medicationId,
			@Valid @RequestBody UpdateMedicationRequest request) {
		return medicationService.update(medicationId, request);
	}

	@PostMapping("/{medicationId}/adjust-stock")
	MedicationResponse adjustMedicationStock(@PathVariable UUID medicationId,
			@Valid @RequestBody StockAdjustmentRequest request) {
		return medicationService.adjustStock(medicationId, request);
	}
}
