package ru.aqstream.notification.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Конфигурация уведомлений.
 */
@ConfigurationProperties(prefix = "notification")
@Validated
@Getter
@Setter
public class NotificationProperties {

    /**
     * Базовый URL для формирования ссылок в уведомлениях.
     */
    private String baseUrl = "https://aqstream.ru";

    /**
     * Формирует URL для события.
     *
     * @param eventSlug slug события
     * @return полный URL
     */
    public String getEventUrl(String eventSlug) {
        return baseUrl + "/events/" + eventSlug;
    }

    /**
     * Формирует URL для организации.
     *
     * @param orgSlug slug организации
     * @return полный URL
     */
    public String getOrganizationUrl(String orgSlug) {
        return baseUrl + "/org/" + orgSlug;
    }
}
