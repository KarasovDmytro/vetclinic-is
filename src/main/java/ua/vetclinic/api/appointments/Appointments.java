package ua.vetclinic.api.appointments;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

/** Доступ інших модулів до записів на прийом без відкриття сховища. */
@Service
public class Appointments {

	private final AppointmentRepository appointmentRepository;

	Appointments(AppointmentRepository appointmentRepository) {
		this.appointmentRepository = appointmentRepository;
	}

	/** Блокує запис до кінця транзакції викликача, щоб паралельні прийоми для нього виконувались по черзі (AD-07.1). */
	public Optional<AppointmentInfo> findForUpdate(UUID appointmentId) {
		return appointmentRepository.findByIdForUpdate(appointmentId).map(AppointmentEntity::toInfo);
	}

	/** Ставить запису статус «Завершено» і зв'язує його з прийомом (AD-03.2); викликається в транзакції збереження прийому. */
	public void complete(UUID appointmentId, UUID visitId) {
		appointmentRepository.findById(appointmentId).orElseThrow().complete(visitId, OffsetDateTime.now());
	}
}
