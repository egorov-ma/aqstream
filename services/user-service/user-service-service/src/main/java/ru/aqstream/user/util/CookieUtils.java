package ru.aqstream.user.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

/**
 * Утилиты для работы с cookies.
 * Используется для безопасного хранения refresh token в httpOnly cookie.
 */
public final class CookieUtils {

    /**
     * Имя cookie для refresh token.
     */
    public static final String REFRESH_TOKEN_COOKIE_NAME = "refresh_token";

    /**
     * Path для cookie (только auth endpoints).
     */
    public static final String COOKIE_PATH = "/api/v1/auth";

    /**
     * Срок жизни cookie в секундах (7 дней).
     */
    public static final int COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private CookieUtils() {
        // Утилитный класс
    }

    /**
     * Устанавливает httpOnly cookie с refresh token.
     *
     * @param response     HTTP response
     * @param refreshToken refresh token для сохранения
     * @param secure       использовать Secure флаг (true для HTTPS)
     */
    public static void setRefreshTokenCookie(
        HttpServletResponse response,
        String refreshToken,
        boolean secure
    ) {
        String cookieHeader = buildCookieHeader(
            REFRESH_TOKEN_COOKIE_NAME,
            refreshToken,
            COOKIE_PATH,
            COOKIE_MAX_AGE_SECONDS,
            secure
        );
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Удаляет cookie с refresh token.
     *
     * @param response HTTP response
     * @param secure   использовать Secure флаг
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response, boolean secure) {
        String cookieHeader = buildCookieHeader(
            REFRESH_TOKEN_COOKIE_NAME,
            "",
            COOKIE_PATH,
            0, // Max-Age=0 удаляет cookie
            secure
        );
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Получает refresh token из cookie.
     *
     * @param request HTTP request
     * @return Optional с refresh token или empty если cookie не найден
     */
    public static Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
            .filter(cookie -> REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName()))
            .map(Cookie::getValue)
            .filter(value -> value != null && !value.isEmpty())
            .findFirst();
    }

    /**
     * Формирует Set-Cookie header с SameSite атрибутом.
     */
    private static String buildCookieHeader(
        String name,
        String value,
        String path,
        int maxAge,
        boolean secure
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("=").append(value);
        sb.append("; Path=").append(path);
        sb.append("; Max-Age=").append(maxAge);
        sb.append("; HttpOnly");
        if (secure) {
            sb.append("; Secure");
            sb.append("; SameSite=Strict");
        } else {
            // В development режиме без HTTPS используем Lax для работы с localhost
            sb.append("; SameSite=Lax");
        }
        return sb.toString();
    }
}
