package ru.aqstream.gateway.version;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Конфигурация URL сервисов для агрегации версий.
 *
 * @param userService         URL User Service
 * @param eventService        URL Event Service
 * @param paymentService      URL Payment Service
 * @param notificationService URL Notification Service
 * @param mediaService        URL Media Service
 * @param analyticsService    URL Analytics Service
 */
@ConfigurationProperties(prefix = "aqstream.services")
public record ServiceEndpoints(
    String userService,
    String eventService,
    String paymentService,
    String notificationService,
    String mediaService,
    String analyticsService
) {

    /**
     * Возвращает URL для получения версии сервиса.
     *
     * @param baseUrl базовый URL сервиса
     * @return URL endpoint версии
     */
    public static String versionUrl(String baseUrl) {
        return baseUrl + "/api/v1/system/version";
    }
}
