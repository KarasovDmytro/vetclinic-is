package ua.vetclinic.api.owners;

import java.util.UUID;
import org.springframework.stereotype.Service;

/** Доступ інших модулів до власників без відкриття сховища. */
@Service
public class OwnerLookup {

  private final OwnerRepository ownerRepository;

  OwnerLookup(OwnerRepository ownerRepository) {
    this.ownerRepository = ownerRepository;
  }

  /**
   * Перевіряє, чи зареєстровано власника (для прив'язки пацієнта).
   *
   * @param ownerId ідентифікатор власника
   * @return {@code true}, якщо власник існує
   */
  public boolean exists(UUID ownerId) {
    return ownerRepository.existsById(ownerId);
  }
}
