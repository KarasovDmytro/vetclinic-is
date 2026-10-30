package ua.vetclinic.api.users;

public record CreateUserRequest(String email, String fullName, Role role, String password) {
}
