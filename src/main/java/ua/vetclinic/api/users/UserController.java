package ua.vetclinic.api.users;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

	private final UserRepository userRepository;

	UserController(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@PostMapping
	ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			throw new EmailTakenException(email);
		}
		UserEntity user = new UserEntity(UUID.randomUUID(), email, request.fullName(), request.role(), true,
				OffsetDateTime.now());
		UserResponse body = userRepository.save(user).toResponse();
		return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
	}

	@PostMapping("/{userId}/deactivate")
	ResponseEntity<UserResponse> deactivateUser(@PathVariable UUID userId) {
		UserEntity user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
		if (!user.isActive()) {
			throw new UserAlreadyDeactivatedException(userId);
		}
		user.deactivate();
		return ResponseEntity.ok(userRepository.save(user).toResponse());
	}

	@GetMapping
	UserPage listUsers(@RequestParam(required = false) Role role, @RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "20") @Max(100) int pageSize) {
		PageRequest pageRequest = PageRequest.of(page - 1, pageSize, Sort.by("fullName"));
		Page<UserEntity> users = role == null ? userRepository.findAll(pageRequest)
				: userRepository.findByRole(role, pageRequest);
		return new UserPage(page, pageSize, users.getTotalElements(),
				users.getContent().stream().map(UserEntity::toResponse).toList());
	}

	@GetMapping("/{userId}")
	ResponseEntity<UserResponse> getUser(@PathVariable UUID userId) {
		return userRepository.findById(userId)
				.map(UserEntity::toResponse)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new UserNotFoundException(userId));
	}
}
