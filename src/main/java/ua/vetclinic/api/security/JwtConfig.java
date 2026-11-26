package ua.vetclinic.api.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import ua.vetclinic.api.users.UserAccounts;

/**
 * Підпис і перевірка JWT алгоритмом HS256 із секретом {@code vetclinic.security.jwt-secret}.
 *
 * <p>Крім підпису й терміну дії, декодер перевіряє, що власник токена досі активний, тому
 * деактивація користувача відкликає його токени одразу (REQ-F-002).
 */
@Configuration
class JwtConfig {

  @Bean
  SecretKey jwtSigningKey(@Value("${vetclinic.security.jwt-secret}") String secret) {
    return new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
  }

  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtSigningKey));
  }

  /**
   * Створює декодер, що разом зі стандартними перевірками (підпис, термін дії) відхиляє токени
   * деактивованих або видалених користувачів.
   *
   * @param jwtSigningKey ключ HS256
   * @param userAccounts джерело ознаки активності користувача
   * @return декодер bearer-токенів
   */
  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtSigningKey, UserAccounts userAccounts) {
    NimbusJwtDecoder decoder =
        NimbusJwtDecoder.withSecretKey(jwtSigningKey).macAlgorithm(MacAlgorithm.HS256).build();
    OAuth2TokenValidator<Jwt> subjectIsActiveUser =
        jwt ->
            userAccounts.isActive(UUID.fromString(jwt.getSubject()))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(
                    new OAuth2Error("invalid_token", "User is not active", null));
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), subjectIsActiveUser));
    return decoder;
  }
}
