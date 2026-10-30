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
import ua.vetclinic.api.users.UserAccount;
import ua.vetclinic.api.users.UserAccounts;

@Service
class AuthService {

	private static final Duration TOKEN_LIFETIME = Duration.ofHours(8);

	private final UserAccounts userAccounts;

	private final JwtEncoder jwtEncoder;

	private final PasswordEncoder passwordEncoder;

	AuthService(UserAccounts userAccounts, JwtEncoder jwtEncoder, PasswordEncoder passwordEncoder) {
		this.userAccounts = userAccounts;
		this.jwtEncoder = jwtEncoder;
		this.passwordEncoder = passwordEncoder;
	}

	LoginResponse login(LoginRequest request) {
		UserAccount account = userAccounts.findByEmail(request.email())
				.orElseThrow(InvalidCredentialsException::new);
		if (!passwordEncoder.matches(request.password(), account.passwordHash())) {
			throw new InvalidCredentialsException();
		}
		Instant issuedAt = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(account.id().toString())
				.claim("role", account.role().name())
				.issuedAt(issuedAt)
				.expiresAt(issuedAt.plus(TOKEN_LIFETIME))
				.build();
		String token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
				.getTokenValue();
		return new LoginResponse(token, "Bearer", TOKEN_LIFETIME.toSeconds(),
				new AuthUser(account.id(), account.email(), account.fullName(), account.role()));
	}
}
