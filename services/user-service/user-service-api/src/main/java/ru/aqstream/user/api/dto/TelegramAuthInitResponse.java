package ru.aqstream.user.api.dto;

import java.time.Instant;

/**
 * Ответ на инициализацию авторизации через Telegram бота.
 *
 * @param token     уникальный токен сессии авторизации
 * @param deeplink  ссылка на бота для подтверждения (t.me/bot?start=auth_XXX)
 * @param expiresAt время истечения токена
 */
public record TelegramAuthInitResponse(
    String token,
    String deeplink,
    Instant expiresAt
) {}
