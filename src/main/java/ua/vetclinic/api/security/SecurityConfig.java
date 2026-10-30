package ua.vetclinic.api.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.ObjectMapper;
import ua.vetclinic.api.common.ErrorResponse;

@Configuration
class SecurityConfig {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder(10);
	}

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		http.authorizeHttpRequests(requests -> requests
				.requestMatchers("/api/v1/users", "/api/v1/users/**").hasRole("ADMIN")
				.anyRequest().authenticated())
				.exceptionHandling(handling -> handling.authenticationEntryPoint((request, response, exception) -> {
					ErrorResponse body = ErrorResponse.of(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
							"Потрібна автентифікація", request);
					response.setStatus(HttpStatus.UNAUTHORIZED.value());
					response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
					response.setContentType(MediaType.APPLICATION_JSON_VALUE);
					response.setCharacterEncoding("UTF-8");
					response.getWriter().write(objectMapper.writeValueAsString(body));
				}));
		return http.build();
	}
}
