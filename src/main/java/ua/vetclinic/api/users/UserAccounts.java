package ua.vetclinic.api.users;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

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

	public boolean isActive(UUID userId) {
		return userRepository.findById(userId).map(UserEntity::isActive).orElse(false);
	}
}
