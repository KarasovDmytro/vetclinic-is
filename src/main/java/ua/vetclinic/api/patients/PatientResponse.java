package ua.vetclinic.api.patients;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PatientResponse(UUID id, UUID ownerId, String name, String species, String breed, Sex sex,
		LocalDate birthDate, PatientStatus status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
}
