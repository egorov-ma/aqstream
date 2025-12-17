package ru.aqstream.user.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Конфигурация JPA для User Service.
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = "ru.aqstream.user.db.repository")
public class JpaConfig {
}
