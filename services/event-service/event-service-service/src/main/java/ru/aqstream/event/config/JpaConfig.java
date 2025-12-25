package ru.aqstream.event.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Конфигурация JPA для Event Service.
 * Включает репозитории и entities из common-messaging для Outbox pattern.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = {
    "ru.aqstream.event.db.repository",
    "ru.aqstream.common.messaging"
})
@EntityScan(basePackages = {
    "ru.aqstream.event.db.entity",
    "ru.aqstream.common.messaging"
})
public class JpaConfig {
}
