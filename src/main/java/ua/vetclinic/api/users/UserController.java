package ua.vetclinic.api.users;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST-ресурс облікових записів {@code /api/v1/users}; доступний лише адміністратору. */
@RestController
@RequestMapping("/api/v1/users")
class UserController {

  private final UserService userService;

  UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * Створює обліковий запис (операція {@code createUser}, REQ-F-001, REQ-F-005).
   *
   * @param request email, ПІБ, роль і пароль
   * @return 201 зі створеним користувачем і заголовком {@code Location}
   */
  @PostMapping
  ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    UserResponse body = userService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/users/" + body.id())).body(body);
  }

  /**
   * Деактивує обліковий запис (операція {@code deactivateUser}, REQ-F-002); видалення користувачів
   * немає.
   *
   * @param userId ідентифікатор користувача
   * @return користувач з {@code active = false}
   */
  @PostMapping("/{userId}/deactivate")
  UserResponse deactivateUser(@PathVariable UUID userId) {
    return userService.deactivate(userId);
  }

  /**
   * Повертає сторінку облікових записів (операція {@code listUsers}, REQ-F-001, REQ-F-005).
   *
   * @param role фільтр за роллю або {@code null}
   * @param active фільтр за активністю або {@code null}
   * @param page номер сторінки, починаючи з 1
   * @param pageSize розмір сторінки, від 1 до 100
   * @return сторінка користувачів, відсортованих за ПІБ
   */
  @GetMapping
  UserPage listUsers(
      @RequestParam(required = false) Role role,
      @RequestParam(required = false) Boolean active,
      @RequestParam(defaultValue = "1") @Min(1) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int pageSize) {
    return userService.list(role, active, page, pageSize);
  }

  /**
   * Повертає обліковий запис за ідентифікатором (операція {@code getUser}, REQ-F-001, REQ-F-005).
   *
   * @param userId ідентифікатор користувача
   * @return користувач
   */
  @GetMapping("/{userId}")
  UserResponse getUser(@PathVariable UUID userId) {
    return userService.get(userId);
  }
}
