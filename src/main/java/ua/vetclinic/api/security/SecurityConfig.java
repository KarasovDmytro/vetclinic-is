package ua.vetclinic.api.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import tools.jackson.databind.ObjectMapper;
import ua.vetclinic.api.common.ErrorResponse;

@Configuration
class SecurityConfig {

	private static JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("role");
		authorities.setAuthorityPrefix("ROLE_");
		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		AuthenticationEntryPoint entryPoint = (request, response, exception) -> {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
			writeError(objectMapper, response,
					ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Потрібна автентифікація", request));
		};
		AccessDeniedHandler accessDeniedHandler = (request, response, exception) -> writeError(objectMapper,
				response, ErrorResponse.of(HttpStatus.FORBIDDEN, "FORBIDDEN", "Недостатньо прав", request));
		http.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
				.requestMatchers("/api/v1/auth/login").permitAll()
				.requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
				.requestMatchers(HttpMethod.GET, "/api/v1/owners", "/api/v1/owners/**")
						.hasAnyRole("ADMIN", "VETERINARIAN")
				.requestMatchers("/api/v1/owners", "/api/v1/owners/**").hasRole("ADMIN")
				.anyRequest().authenticated())
				.oauth2ResourceServer(resourceServer -> resourceServer
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler)
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
				.exceptionHandling(handling -> handling
						.authenticationEntryPoint(entryPoint)
						.accessDeniedHandler(accessDeniedHandler));
		return http.build();
	}

	private static void writeError(ObjectMapper objectMapper, HttpServletResponse response, ErrorResponse body)
			throws IOException {
		response.setStatus(body.status());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
