package ru.aqstream.notification.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Конфигурация Telegram бота.
 */
@ConfigurationProperties(prefix = "telegram")
@Validated
@Getter
@Setter
public class TelegramProperties {

    /**
     * Токен бота, полученный от @BotFather.
     */
    @NotBlank(message = "Telegram bot token обязателен")
    private String botToken;

    /**
     * Username бота (без @).
     */
    @NotBlank(message = "Telegram bot username обязателен")
    private String botUsername = "AqStreamBot";

    /**
     * URL для webhook. Если пусто — используется long polling.
     */
    private String webhookUrl;

    /**
     * Базовый URL для deeplinks.
     */
    private String deeplinkBaseUrl = "https://t.me/";

    /**
     * Таймаут для long polling в секундах.
     */
    private int longPollingTimeout = 30;

    /**
     * Использовать ли webhook вместо long polling.
     */
    public boolean isWebhookEnabled() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    /**
     * Формирует полный URL для deeplink.
     */
    public String getDeeplinkUrl(String startParam) {
        return deeplinkBaseUrl + botUsername + "?start=" + startParam;
    }
}
