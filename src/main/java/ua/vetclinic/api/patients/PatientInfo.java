package ua.vetclinic.api.patients;

import java.util.UUID;

/** Пацієнт для інших модулів: ідентифікатор, ім'я та ознака архівної картки. */
public record PatientInfo(UUID id, String name, boolean archived) {
}
