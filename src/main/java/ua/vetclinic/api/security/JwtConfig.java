package ua.vetclinic.api.security;

import static java.nio.charset.StandardCharsets.UTF_8;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration
class JwtConfig {

	@Bean
	JwtEncoder jwtEncoder(@Value("${vetclinic.security.jwt-secret}") String secret) {
		SecretKey key = new SecretKeySpec(secret.getBytes(UTF_8), "HmacSHA256");
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}
}
