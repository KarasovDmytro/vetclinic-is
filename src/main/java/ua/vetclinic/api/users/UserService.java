package ua.vetclinic.api.users;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	UserResponse create(CreateUserRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			throw new EmailTakenException(email);
		}
		UserEntity user = new UserEntity(UUID.randomUUID(), email, request.fullName(),
				passwordEncoder.encode(request.password()), request.role(), true, OffsetDateTime.now());
		return userRepository.save(user).toResponse();
	}

	UserResponse deactivate(UUID userId) {
		UserEntity user = findOrThrow(userId);
		if (!user.isActive()) {
			throw new UserAlreadyDeactivatedException(userId);
		}
		if (user.isAdmin() && userRepository.countByRoleAndActive(Role.ADMIN, true) <= 1) {
			throw new LastAdminException();
		}
		user.deactivate();
		return userRepository.save(user).toResponse();
	}

	UserResponse get(UUID userId) {
		return findOrThrow(userId).toResponse();
	}

	UserPage list(Role role, Boolean active, int page, int pageSize) {
		Page<UserEntity> users = userRepository.search(role, active,
				PageRequest.of(page - 1, pageSize, Sort.by("fullName")));
		return new UserPage(page, pageSize, users.getTotalElements(),
				users.getContent().stream().map(UserEntity::toResponse).toList());
	}

	private UserEntity findOrThrow(UUID userId) {
		return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
	}
}
