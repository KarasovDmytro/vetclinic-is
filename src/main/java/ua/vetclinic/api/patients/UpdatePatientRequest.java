package ua.vetclinic.api.patients;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.Size;
import ua.vetclinic.api.common.ErrorDetail;
import ua.vetclinic.api.common.Strings;

/**
 * PATCH: сеттери запам'ятовують, які поля прийшли (відсутнє поле і явний null — різні речі). Поля ownerId та status
 * приймаються лише для того, щоб відхилити їх як незмінювані (REQ-F-014).
 */
public class UpdatePatientRequest {

	private final Set<String> provided = new HashSet<>();

	private final Set<String> immutableProvided = new LinkedHashSet<>();

	@Size(min = 1, max = 100)
	private String name;

	@Size(min = 1, max = 50)
	private String species;

	@Size(min = 1, max = 100)
	private String breed;

	private Sex sex;

	private LocalDate birthDate;

	public void setName(String name) {
		this.name = Strings.strip(name);
		provided.add("name");
	}

	public void setSpecies(String species) {
		this.species = Strings.strip(species);
		provided.add("species");
	}

	public void setBreed(String breed) {
		this.breed = Strings.strip(breed);
		provided.add("breed");
	}

	public void setSex(Sex sex) {
		this.sex = sex;
		provided.add("sex");
	}

	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
		provided.add("birthDate");
	}

	public void setOwnerId(Object ignored) {
		immutableProvided.add("ownerId");
	}

	public void setStatus(Object ignored) {
		immutableProvided.add("status");
	}

	String name() {
		return name;
	}

	String species() {
		return species;
	}

	String breed() {
		return breed;
	}

	Sex sex() {
		return sex;
	}

	LocalDate birthDate() {
		return birthDate;
	}

	boolean isProvided(String field) {
		return provided.contains(field);
	}

	boolean isEmpty() {
		return provided.isEmpty() && immutableProvided.isEmpty();
	}

	List<ErrorDetail> violations() {
		List<ErrorDetail> details = new ArrayList<>();
		for (String field : immutableProvided) {
			details.add(new ErrorDetail(field, "IMMUTABLE_FIELD", "Поле не можна змінити"));
		}
		if (isProvided("name") && name == null) {
			details.add(new ErrorDetail("name", "REQUIRED", "Поле обов'язкове"));
		}
		if (isProvided("species") && species == null) {
			details.add(new ErrorDetail("species", "REQUIRED", "Поле обов'язкове"));
		}
		if (isProvided("sex") && sex == null) {
			details.add(new ErrorDetail("sex", "REQUIRED", "Поле обов'язкове"));
		}
		return details;
	}
}
