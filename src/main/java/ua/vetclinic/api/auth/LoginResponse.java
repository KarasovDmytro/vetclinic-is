package ua.vetclinic.api.auth;

/**
 * Відповідь на успішний вхід.
 *
 * @param accessToken підписаний JWT
 * @param tokenType завжди {@code Bearer}
 * @param expiresIn термін дії токена, секунд
 * @param user автентифікований користувач
 */
record LoginResponse(String accessToken, String tokenType, long expiresIn, AuthUser user) {}
