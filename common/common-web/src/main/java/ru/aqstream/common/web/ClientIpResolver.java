package ru.aqstream.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Утилита для получения IP адреса клиента.
 *
 * <p>Учитывает прокси-серверы (nginx, load balancer) и извлекает
 * реальный IP из заголовков X-Forwarded-For или X-Real-IP.</p>
 *
 * <p>Пример использования:</p>
 * <pre>
 * String clientIp = ClientIpResolver.resolve(httpServletRequest);
 * </pre>
 */
public final class ClientIpResolver {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private ClientIpResolver() {
        // Утилитный класс
    }

    /**
     * Получает IP адрес клиента с учётом прокси.
     *
     * <p>Порядок проверки:</p>
     * <ol>
     *   <li>X-Forwarded-For (первый IP в списке)</li>
     *   <li>X-Real-IP</li>
     *   <li>RemoteAddr</li>
     * </ol>
     *
     * @param request HTTP запрос
     * @return IP адрес клиента
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // X-Forwarded-For может содержать список IP через запятую
        String xForwardedFor = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Берём первый IP (оригинальный клиент)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader(HEADER_X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        return request.getRemoteAddr();
    }
}
