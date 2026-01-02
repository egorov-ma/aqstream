package ru.aqstream.user.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Ответ на проверку статуса авторизации через Telegram бота.
 *
 * <p>Статусы:
 * <ul>
 *   <li>PENDING — ожидание подтверждения в боте</li>
 *   <li>CONFIRMED — авторизация подтверждена, содержит JWT токены</li>
 *   <li>EXPIRED — токен истёк</li>
 * </ul>
 *
 * @param status      текущий статус токена
 * @param accessToken JWT access token (только при CONFIRMED)
 * @param expiresIn   время жизни access token в секундах (только при CONFIRMED)
 * @param tokenType   тип токена (Bearer, только при CONFIRMED)
 * @param user        данные пользователя (только при CONFIRMED)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TelegramAuthStatusResponse(
    String status,
    String accessToken,
    Long expiresIn,
    String tokenType,
    UserDto user
) {

    /**
     * Создаёт ответ для статуса PENDING.
     *
     * @return TelegramAuthStatusResponse с status=PENDING
     */
    public static TelegramAuthStatusResponse pending() {
        return new TelegramAuthStatusResponse("PENDING", null, null, null, null);
    }

    /**
     * Создаёт ответ для статуса EXPIRED.
     *
     * @return TelegramAuthStatusResponse с status=EXPIRED
     */
    public static TelegramAuthStatusResponse expired() {
        return new TelegramAuthStatusResponse("EXPIRED", null, null, null, null);
    }

    /**
     * Создаёт ответ для статуса CONFIRMED с JWT токенами.
     *
     * @param accessToken JWT access token
     * @param expiresIn   время жизни в секундах
     * @param user        данные пользователя
     * @return TelegramAuthStatusResponse с токенами
     */
    public static TelegramAuthStatusResponse confirmed(String accessToken, long expiresIn, UserDto user) {
        return new TelegramAuthStatusResponse("CONFIRMED", accessToken, expiresIn, "Bearer", user);
    }
}
