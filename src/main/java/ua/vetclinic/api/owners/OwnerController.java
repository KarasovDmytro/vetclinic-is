package ua.vetclinic.api.owners;

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
@RequestMapping("/api/v1/owners")
class OwnerController {

	private final OwnerService ownerService;

	OwnerController(OwnerService ownerService) {
		this.ownerService = ownerService;
	}

	@PostMapping
	ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody CreateOwnerRequest request) {
		OwnerResponse body = ownerService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/owners/" + body.id())).body(body);
	}

	@GetMapping
	OwnerPage listOwners(@RequestParam(required = false) @Size(min = 2, max = 100) String q,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return ownerService.list(q, page, pageSize);
	}

	@GetMapping("/{ownerId}")
	OwnerResponse getOwner(@PathVariable UUID ownerId) {
		return ownerService.get(ownerId);
	}

	@PatchMapping("/{ownerId}")
	OwnerResponse updateOwner(@PathVariable UUID ownerId, @Valid @RequestBody UpdateOwnerRequest request) {
		return ownerService.update(ownerId, request);
	}
}
