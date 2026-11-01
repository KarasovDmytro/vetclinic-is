package ua.vetclinic.api.auth;

record LoginResponse(String accessToken, String tokenType, long expiresIn, AuthUser user) {
}
