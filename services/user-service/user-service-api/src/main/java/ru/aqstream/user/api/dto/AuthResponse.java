package ru.aqstream.user.api.dto;

/**
 * Ответ с токенами аутентификации.
 *
 * @param accessToken  JWT access token (срок жизни 15 минут)
 * @param refreshToken JWT refresh token (срок жизни 7 дней)
 * @param expiresIn    время жизни access token в секундах
 * @param tokenType    тип токена (Bearer)
 * @param user         данные пользователя
 */
public record AuthResponse(
    String accessToken,
    String refreshToken,
    long expiresIn,
    String tokenType,
    UserDto user
) {
    /**
     * Создаёт AuthResponse с типом Bearer.
     *
     * @param accessToken  JWT access token
     * @param refreshToken JWT refresh token
     * @param expiresIn    время жизни в секундах
     * @param user         данные пользователя
     * @return AuthResponse
     */
    public static AuthResponse bearer(String accessToken, String refreshToken, long expiresIn, UserDto user) {
        return new AuthResponse(accessToken, refreshToken, expiresIn, "Bearer", user);
    }
}
