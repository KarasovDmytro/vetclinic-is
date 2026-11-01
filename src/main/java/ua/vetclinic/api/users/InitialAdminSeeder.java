package ua.vetclinic.api.users;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class InitialAdminSeeder implements ApplicationRunner {

	private final UserService userService;

	private final UserRepository userRepository;

	private final String email;

	private final String password;

	InitialAdminSeeder(UserService userService, UserRepository userRepository,
			@Value("${vetclinic.bootstrap.admin-email:}") String email,
			@Value("${vetclinic.bootstrap.admin-password:}") String password) {
		this.userService = userService;
		this.userRepository = userRepository;
		this.email = email;
		this.password = password;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (email.isBlank() || password.isBlank() || userRepository.existsByEmail(email.toLowerCase(Locale.ROOT))) {
			return;
		}
		userService.create(new CreateUserRequest(email, "Початковий адміністратор", Role.ADMIN, password));
	}
}
