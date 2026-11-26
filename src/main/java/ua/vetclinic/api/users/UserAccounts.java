package ua.vetclinic.api.users;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Доступ модулів автентифікації та безпеки до облікових записів без відкриття сховища. */
@Service
public class UserAccounts {

  private final UserRepository userRepository;

  UserAccounts(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Знаходить обліковий запис за email без урахування регістру.
   *
   * @param email email із запиту на вхід
   * @return обліковий запис або порожньо
   */
  public Optional<UserAccount> findByEmail(String email) {
    return userRepository.findByEmail(email.toLowerCase(Locale.ROOT)).map(UserEntity::toAccount);
  }

  /**
   * Перевіряє, чи користувач існує й не деактивований (перевірка кожного токена).
   *
   * @param userId ідентифікатор користувача з claim {@code sub}
   * @return {@code true}, якщо користувач активний
   */
  public boolean isActive(UUID userId) {
    return userRepository.findById(userId).map(UserEntity::isActive).orElse(false);
  }
}
