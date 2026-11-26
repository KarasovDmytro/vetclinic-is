package ua.vetclinic.api.users;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Довідник лікарів: користувачі з роллю VETERINARIAN (AD-06.3). */
@Service
public class Doctors {

  private final UserRepository userRepository;

  Doctors(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  /**
   * Повертає активних лікарів для довідника й вибору лікаря під час запису.
   *
   * @return активні лікарі, відсортовані за ПІБ
   */
  public List<DoctorInfo> listActive() {
    return userRepository.findByRoleAndActiveTrueOrderByFullName(Role.VETERINARIAN).stream()
        .map(UserEntity::toDoctorInfo)
        .toList();
  }

  /**
   * Знаходить лікаря, зокрема деактивованого, щоб викликач міг розрізнити «не знайдено» і
   * «деактивовано».
   *
   * @param doctorId ідентифікатор користувача
   * @return лікар або порожньо, якщо такого користувача немає чи він не має ролі {@code
   *     VETERINARIAN}
   */
  public Optional<DoctorInfo> find(UUID doctorId) {
    return userRepository
        .findById(doctorId)
        .filter(UserEntity::isVeterinarian)
        .map(UserEntity::toDoctorInfo);
  }
}
