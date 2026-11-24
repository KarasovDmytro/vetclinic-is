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
import ua.vetclinic.api.common.ConflictException;
import ua.vetclinic.api.common.ResourceNotFoundException;

/**
 * Бізнес-логіка облікових записів: створення з хешуванням пароля, деактивація з захистом останнього
 * адміністратора та перегляд.
 */
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

  /**
   * Створює активний обліковий запис.
   *
   * @param request email, ПІБ, роль і пароль
   * @return створений користувач
   * @throws ConflictException {@code EMAIL_TAKEN}, якщо email уже зайнятий (без урахування
   *     регістру)
   */
  UserResponse create(CreateUserRequest request) {
    String email = request.email().toLowerCase(Locale.ROOT);
    if (userRepository.existsByEmail(email)) {
      auditLog.failure("USER_CREATED", "USER", null, "EMAIL_TAKEN");
      throw new ConflictException("EMAIL_TAKEN", "Email вже зайнятий");
    }
    UserEntity user =
        new UserEntity(
            UUID.randomUUID(),
            email,
            request.fullName(),
            passwordEncoder.encode(request.password()),
            request.role(),
            true,
            OffsetDateTime.now());
    UserResponse created = userRepository.save(user).toResponse();
    auditLog.success("USER_CREATED", "USER", created.id());
    return created;
  }

  /**
   * Деактивує обліковий запис.
   *
   * @param userId ідентифікатор користувача
   * @return деактивований користувач
   * @throws ResourceNotFoundException якщо користувача немає
   * @throws ConflictException {@code USER_ALREADY_DEACTIVATED} для вже деактивованого або {@code
   *     LAST_ADMIN} для останнього активного адміністратора
   */
  UserResponse deactivate(UUID userId) {
    UserEntity user = findOrThrow(userId);
    if (!user.isActive()) {
      auditLog.failure("USER_DEACTIVATED", "USER", userId, "USER_ALREADY_DEACTIVATED");
      throw new ConflictException("USER_ALREADY_DEACTIVATED", "Користувача вже деактивовано");
    }
    if (user.isAdmin() && userRepository.countByRoleAndActive(Role.ADMIN, true) <= 1) {
      auditLog.failure("USER_DEACTIVATED", "USER", userId, "LAST_ADMIN");
      throw new ConflictException("LAST_ADMIN", "Не можна деактивувати останнього адміністратора");
    }
    user.deactivate();
    UserResponse deactivated = userRepository.save(user).toResponse();
    auditLog.success("USER_DEACTIVATED", "USER", userId);
    return deactivated;
  }

  /**
   * Повертає обліковий запис за ідентифікатором.
   *
   * @param userId ідентифікатор користувача
   * @return користувач
   * @throws ResourceNotFoundException якщо користувача немає
   */
  UserResponse get(UUID userId) {
    return findOrThrow(userId).toResponse();
  }

  /**
   * Повертає сторінку облікових записів за фільтрами.
   *
   * @param role фільтр за роллю або {@code null}
   * @param active фільтр за активністю або {@code null}
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки
   * @return сторінка користувачів, відсортованих за ПІБ
   */
  UserPage list(Role role, Boolean active, int page, int pageSize) {
    Page<UserEntity> users =
        userRepository.search(
            role, active, PageRequest.of(page - 1, pageSize, Sort.by("fullName", "id")));
    return new UserPage(
        page,
        pageSize,
        users.getTotalElements(),
        users.getContent().stream().map(UserEntity::toResponse).toList());
  }

  private UserEntity findOrThrow(UUID userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
  }
}
