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

	public List<DoctorInfo> listActive() {
		return userRepository.findByRoleAndActiveTrueOrderByFullName(Role.VETERINARIAN).stream()
				.map(UserEntity::toDoctorInfo).toList();
	}

	public Optional<DoctorInfo> find(UUID doctorId) {
		return userRepository.findById(doctorId).filter(UserEntity::isVeterinarian).map(UserEntity::toDoctorInfo);
	}
}
