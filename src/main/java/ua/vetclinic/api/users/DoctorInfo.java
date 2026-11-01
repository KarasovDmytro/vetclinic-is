package ua.vetclinic.api.users;

import java.util.UUID;

/** Лікар для інших модулів: ідентифікатор, ПІБ і ознака активності. */
public record DoctorInfo(UUID id, String fullName, boolean active) {
}
