package ua.vetclinic.api.auth;

import java.time.Duration;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import ua.vetclinic.api.audit.AuditLog;
import ua.vetclinic.api.users.UserAccount;
import ua.vetclinic.api.users.UserAccounts;

/**
 * Вхід у систему: перевірка облікових даних і випуск JWT із терміном дії 8 годин (addendum-001).
 *
 * <p>Невідомий email і хибний пароль дають однакову помилку, щоб не розкривати наявність облікового
 * запису. Кожна спроба фіксується в журналі аудиту.
 */
@Service
class AuthService {

  private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);

  private final UserAccounts userAccounts;

  private final JwtEncoder jwtEncoder;

  private final PasswordEncoder passwordEncoder;

  private final AuditLog auditLog;

  AuthService(
      UserAccounts userAccounts,
      JwtEncoder jwtEncoder,
      PasswordEncoder passwordEncoder,
      AuditLog auditLog) {
    this.userAccounts = userAccounts;
    this.jwtEncoder = jwtEncoder;
    this.passwordEncoder = passwordEncoder;
    this.auditLog = auditLog;
  }

  /**
   * Перевіряє облікові дані та видає токен із claim {@code role}.
   *
   * @param request email і пароль
   * @return токен, його тип, термін дії в секундах і дані користувача
   * @throws InvalidCredentialsException якщо email не знайдено або пароль хибний
   * @throws AccountDeactivatedException якщо обліковий запис деактивовано
   */
  LoginResponse login(LoginRequest request) {
    UserAccount account = userAccounts.findByEmail(request.email()).orElse(null);
    if (account == null) {
      auditLog.failure("LOGIN_FAILED", null, null, "USER", null, "INVALID_CREDENTIALS");
      throw new InvalidCredentialsException();
    }
    if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
      auditLog.failure(
          "LOGIN_FAILED",
          account.id(),
          account.role().name(),
          "USER",
          account.id(),
          "INVALID_CREDENTIALS");
      throw new InvalidCredentialsException();
    }
    if (!account.active()) {
      auditLog.failure(
          "LOGIN_FAILED",
          account.id(),
          account.role().name(),
          "USER",
          account.id(),
          "ACCOUNT_DEACTIVATED");
      throw new AccountDeactivatedException();
    }
    auditLog.success("LOGIN_SUCCESS", account.id(), account.role().name(), "USER", account.id());
    Instant issuedAt = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .subject(account.id().toString())
            .claim("role", account.role().name())
            .issuedAt(issuedAt)
            .expiresAt(issuedAt.plus(TOKEN_LIFETIME))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new LoginResponse(
        token,
        "Bearer",
        TOKEN_LIFETIME.toSeconds(),
        new AuthUser(account.id(), account.email(), account.fullName(), account.role()));
  }
}
