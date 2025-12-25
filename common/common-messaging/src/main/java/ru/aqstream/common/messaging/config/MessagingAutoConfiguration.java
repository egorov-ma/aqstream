package ru.aqstream.common.messaging.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.ComponentScan;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.messaging.OutboxRepository;

/**
 * Auto-configuration для Outbox pattern messaging.
 *
 * <p>Выполняет явное сканирование компонентов пакета messaging,
 * обходя проблему со сканированием в nested JARs.</p>
 *
 * <p>Включает:</p>
 * <ul>
 *   <li>{@link EventPublisher} — публикация событий через Outbox</li>
 *   <li>{@link ru.aqstream.common.messaging.OutboxProcessor} — обработка и отправка событий</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnClass({OutboxRepository.class})
@ComponentScan(basePackages = "ru.aqstream.common.messaging")
public class MessagingAutoConfiguration {
    // Бины создаются через @ComponentScan
}
