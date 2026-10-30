package ua.vetclinic.api.owners;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("/{ownerId}")
	OwnerResponse getOwner(@PathVariable UUID ownerId) {
		return ownerService.get(ownerId);
	}
}
