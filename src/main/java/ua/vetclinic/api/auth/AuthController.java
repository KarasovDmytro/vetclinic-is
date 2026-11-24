package ua.vetclinic.api.auth;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST-ресурс автентифікації {@code /api/v1/auth}; єдиний ендпоінт, доступний без токена. */
@RestController
@RequestMapping("/api/v1/auth")
class AuthController {

  private final AuthService authService;

  AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * Видає bearer-токен за email і паролем (операція {@code login}, REQ-F-003).
   *
   * @param request облікові дані
   * @return 200 з токеном і даними користувача
   */
  @PostMapping("/login")
  ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }
}
