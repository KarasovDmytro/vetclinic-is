package ua.vetclinic.api.users;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

	private final UserRepository userRepository;

	UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@PostMapping
	@PreAuthorize("hasRole('ADMIN')")
	ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new EmailTakenException(request.email());
		}
		UserEntity user = new UserEntity(UUID.randomUUID(), request.email(), request.fullName(), request.role(), true,
				OffsetDateTime.now());
		UserResponse body = userRepository.save(user).toResponse();
		return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
	}

	@GetMapping("/{userId}")
	ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
		return userRepository.findById(userId)
				.map(UserEntity::toResponse)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
}
