package ua.vetclinic.api.visits;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.Strings;

/**
 * PATCH: сеттери запам'ятовують, які поля прийшли. Поле medications приймається лише для того, щоб відхилити його як
 * незмінюване: перелік препаратів після збереження не змінюється (AD-07.5).
 */
public class UpdateVisitRequest {

	private final Set<String> provided = new HashSet<>();

	private boolean medicationsProvided;

	@Size(min = 1, max = 2000)
	private String diagnosis;

	@Size(min = 1, max = 4000)
	private String prescriptions;

	@Size(min = 1, max = 4000)
	private String notes;

	public void setDiagnosis(String diagnosis) {
		this.diagnosis = Strings.strip(diagnosis);
		provided.add("diagnosis");
	}

	public void setPrescriptions(String prescriptions) {
		this.prescriptions = Strings.strip(prescriptions);
		provided.add("prescriptions");
	}

	public void setNotes(String notes) {
		this.notes = Strings.strip(notes);
		provided.add("notes");
	}

	public void setMedications(Object ignored) {
		medicationsProvided = true;
	}

	String diagnosis() {
		return diagnosis;
	}

	String prescriptions() {
		return prescriptions;
	}

	String notes() {
		return notes;
	}

	boolean isProvided(String field) {
		return provided.contains(field);
	}

	boolean isEmpty() {
		return provided.isEmpty() && !medicationsProvided;
	}

	List<ErrorDetail> violations() {
		List<ErrorDetail> details = new ArrayList<>();
		if (medicationsProvided) {
			details.add(new ErrorDetail("medications", "IMMUTABLE_FIELD", "Поле не можна змінити"));
		}
		if (isProvided("diagnosis") && diagnosis == null) {
			details.add(new ErrorDetail("diagnosis", "REQUIRED", "Поле обов'язкове"));
		}
		return details;
	}
}
