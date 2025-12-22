package ru.aqstream.notification.config;

import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация Telegram бота.
 */
@Configuration
@EnableConfigurationProperties(TelegramProperties.class)
@RequiredArgsConstructor
@Slf4j
public class TelegramBotConfig {

    private final TelegramProperties telegramProperties;

    /**
     * Создаёт экземпляр Telegram бота.
     */
    @Bean
    public TelegramBot telegramBot() {
        log.info("Инициализация Telegram бота: username={}", telegramProperties.getBotUsername());

        TelegramBot bot = new TelegramBot(telegramProperties.getBotToken());

        if (telegramProperties.isWebhookEnabled()) {
            log.info("Telegram бот настроен на webhook: url={}", telegramProperties.getWebhookUrl());
        } else {
            log.info("Telegram бот настроен на long polling");
        }

        return bot;
    }
}
