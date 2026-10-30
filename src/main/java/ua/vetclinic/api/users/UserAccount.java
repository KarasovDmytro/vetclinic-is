package ua.vetclinic.api.users;

import java.util.UUID;

public record UserAccount(UUID id, String email, String fullName, Role role, boolean active, String passwordHash) {
}
