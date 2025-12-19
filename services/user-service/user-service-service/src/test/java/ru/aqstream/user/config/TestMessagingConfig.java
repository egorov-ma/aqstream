package ru.aqstream.user.config;

import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import ru.aqstream.common.messaging.EventPublisher;

/**
 * Тестовая конфигурация для интеграционных тестов.
 * Предоставляет mock EventPublisher вместо реального.
 */
@TestConfiguration
public class TestMessagingConfig {

    /**
     * Mock EventPublisher для интеграционных тестов.
     * Заменяет реальный EventPublisher, который требует RabbitMQ.
     */
    @Bean
    @Primary
    public EventPublisher eventPublisher() {
        return Mockito.mock(EventPublisher.class);
    }
}
