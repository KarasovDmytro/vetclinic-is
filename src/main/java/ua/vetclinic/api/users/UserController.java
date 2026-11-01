package ua.vetclinic.api.users;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

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

	private final UserService userService;

	UserController(UserService userService) {
		this.userService = userService;
	}

	@PostMapping
	ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
		UserResponse body = userService.create(request);
		return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
	}

	@PostMapping("/{userId}/deactivate")
	UserResponse deactivateUser(@PathVariable UUID userId) {
		return userService.deactivate(userId);
	}

	@GetMapping
	UserPage listUsers(@RequestParam(required = false) Role role, @RequestParam(required = false) Boolean active,
			@RequestParam(defaultValue = "1") @Min(1) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
		return userService.list(role, active, page, pageSize);
	}

	@GetMapping("/{userId}")
	UserResponse getUser(@PathVariable UUID userId) {
		return userService.get(userId);
	}
}
