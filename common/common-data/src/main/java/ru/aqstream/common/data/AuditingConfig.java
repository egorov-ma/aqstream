package ru.aqstream.common.data;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Конфигурация JPA Auditing для автоматического заполнения createdAt и updatedAt.
 *
 * <p>Эта конфигурация должна быть импортирована в каждый сервис, использующий common-data:</p>
 * <pre>
 * {@code @Import(AuditingConfig.class)}
 * </pre>
 */
@Configuration
@EnableJpaAuditing
public class AuditingConfig {
    // Конфигурация через аннотацию @EnableJpaAuditing
}
