package ua.vetclinic.api.auth;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(@NotBlank String email, @NotBlank String password) {
}
