package ua.vetclinic.api.users;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;

@Service
class UserService {

	private final UserRepository userRepository;

	private final PasswordEncoder passwordEncoder;

	private final AuditLog auditLog;

	UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuditLog auditLog) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.auditLog = auditLog;
	}

	UserResponse create(CreateUserRequest request) {
		String email = request.email().toLowerCase(Locale.ROOT);
		if (userRepository.existsByEmail(email)) {
			auditLog.failure("USER_CREATED", "USER", null, "EMAIL_TAKEN");
			throw new EmailTakenException(email);
		}
		UserEntity user = new UserEntity(UUID.randomUUID(), email, request.fullName(),
				passwordEncoder.encode(request.password()), request.role(), true, OffsetDateTime.now());
		UserResponse created = userRepository.save(user).toResponse();
		auditLog.success("USER_CREATED", "USER", created.id());
		return created;
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
		UserResponse deactivated = userRepository.save(user).toResponse();
		auditLog.success("USER_DEACTIVATED", "USER", userId);
		return deactivated;
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
