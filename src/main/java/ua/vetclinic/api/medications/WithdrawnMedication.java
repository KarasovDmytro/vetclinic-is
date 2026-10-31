package ua.vetclinic.api.medications;

import java.util.UUID;

/** Результат списання препарату: назва, списана кількість, залишок після списання та поріг. */
public record WithdrawnMedication(UUID medicationId, String name, int quantity, int remaining, Integer minThreshold) {

	/** Залишок після списання строго нижчий за заданий поріг (AD-02.2). */
	public boolean isLow() {
		return minThreshold != null && remaining < minThreshold;
	}
}
