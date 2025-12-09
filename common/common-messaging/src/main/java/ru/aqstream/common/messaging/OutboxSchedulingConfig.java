package ru.aqstream.common.messaging;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Конфигурация для включения планировщика задач.
 * Необходима для работы {@link OutboxProcessor}.
 *
 * <p>Эта конфигурация автоматически подключается при сканировании компонентов.
 * Если в сервисе уже есть своя конфигурация с @EnableScheduling,
 * эта будет проигнорирована.</p>
 */
@Configuration
@EnableScheduling
public class OutboxSchedulingConfig {
    // Конфигурация через аннотацию @EnableScheduling
}
