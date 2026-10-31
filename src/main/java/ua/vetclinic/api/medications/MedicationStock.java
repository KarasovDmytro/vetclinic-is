package ua.vetclinic.api.medications;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import ua.vetclinic.api.common.ConflictException;
import ua.vetclinic.api.common.ErrorDetail;

/** Доступ інших модулів до залишків без відкриття сховища; списання виконується в транзакції викликача. */
@Service
public class MedicationStock {

	private final MedicationRepository medicationRepository;

	MedicationStock(MedicationRepository medicationRepository) {
		this.medicationRepository = medicationRepository;
	}

	/** Ідентифікатори, яких немає в каталозі. Сутності не завантажуються, щоб блокувальне читання при списанні бачило свіжий залишок. */
	public Set<UUID> missing(Collection<UUID> medicationIds) {
		Set<UUID> missing = new HashSet<>(medicationIds);
		missing.removeAll(medicationRepository.findExistingIds(medicationIds));
		return missing;
	}

	/**
	 * Списує препарати все або нічого: якщо хоч одного не вистачає, залишки не змінюються, а виняток описує кожен
	 * проблемний препарат (REQ-F-046). Рядки блокуються до кінця транзакції викликача, тому паралельні списання
	 * не можуть перевищити залишок (REQ-F-048).
	 */
	public List<WithdrawnMedication> withdraw(List<StockRequest> requests) {
		Map<UUID, MedicationEntity> locked = new HashMap<>();
		medicationRepository.findAllForUpdate(requests.stream().map(StockRequest::medicationId).toList())
				.forEach(medication -> locked.put(medication.id(), medication));
		List<ErrorDetail> shortages = new ArrayList<>();
		for (int i = 0; i < requests.size(); i++) {
			StockRequest request = requests.get(i);
			MedicationEntity medication = locked.get(request.medicationId());
			if (medication.quantity() < request.quantity()) {
				String message = "Недостатній залишок: %s (доступно: %d, запитано: %d)".formatted(medication.name(),
						medication.quantity(), request.quantity());
				Map<String, Object> params = new LinkedHashMap<>();
				params.put("medicationId", request.medicationId());
				params.put("name", medication.name());
				params.put("available", medication.quantity());
				params.put("requested", request.quantity());
				shortages.add(new ErrorDetail("medications[" + i + "].quantity", "INSUFFICIENT_STOCK", message, params));
			}
		}
		if (!shortages.isEmpty()) {
			throw new ConflictException("INSUFFICIENT_STOCK",
					shortages.stream().map(ErrorDetail::message).collect(Collectors.joining("; ")), shortages);
		}
		OffsetDateTime now = OffsetDateTime.now();
		List<WithdrawnMedication> withdrawn = new ArrayList<>();
		for (StockRequest request : requests) {
			MedicationEntity medication = locked.get(request.medicationId());
			medication.adjust(-request.quantity(), now);
			withdrawn.add(new WithdrawnMedication(medication.id(), medication.name(), request.quantity(),
					medication.quantity(), medication.minThreshold()));
		}
		return withdrawn;
	}
}
