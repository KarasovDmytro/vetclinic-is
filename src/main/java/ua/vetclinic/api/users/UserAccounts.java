package ua.vetclinic.api.users;

import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class UserAccounts {

	private final UserRepository userRepository;

	UserAccounts(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	public Optional<UserAccount> findByEmail(String email) {
		return userRepository.findByEmail(email.toLowerCase(Locale.ROOT)).map(UserEntity::toAccount);
	}
}
