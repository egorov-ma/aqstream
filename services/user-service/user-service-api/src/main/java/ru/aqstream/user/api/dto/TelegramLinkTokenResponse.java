package ru.aqstream.user.api.dto;

/**
 * Ответ с токеном для привязки Telegram.
 *
 * @param token   токен для привязки
 * @param botLink ссылка на бота с deeplink
 */
public record TelegramLinkTokenResponse(String token, String botLink) {}
