package ru.aqstream.event.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Конфигурация JPA для Event Service.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "ru.aqstream.event.db.repository")
public class JpaConfig {
}
