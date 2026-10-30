package ua.vetclinic.api.users;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
class UserController {

	@PostMapping
	ResponseEntity<UserResponse> createUser(@RequestBody CreateUserRequest request) {
		UUID id = UUID.randomUUID();
		UserResponse body = new UserResponse(id, request.email(), request.fullName(), request.role(), true,
				OffsetDateTime.now());
		return ResponseEntity.created(URI.create("/api/v1/users/" + id)).body(body);
	}
}
