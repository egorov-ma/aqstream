package ru.aqstream.gateway;

/**
 * Константы HTTP заголовков для Gateway.
 */
public final class GatewayHeaders {

    private GatewayHeaders() {
        // Utility class
    }

    /**
     * ID пользователя из JWT токена.
     */
    public static final String USER_ID = "X-User-Id";

    /**
     * ID tenant'а из JWT токена.
     */
    public static final String TENANT_ID = "X-Tenant-Id";

    /**
     * Роли пользователя через запятую.
     */
    public static final String USER_ROLES = "X-User-Roles";

    /**
     * ID для трейсинга запроса.
     */
    public static final String CORRELATION_ID = "X-Correlation-ID";
}
