package ua.vetclinic.api.auth;

import java.util.UUID;

import ua.vetclinic.api.users.Role;

record AuthUser(UUID id, String email, String fullName, Role role) {
}
