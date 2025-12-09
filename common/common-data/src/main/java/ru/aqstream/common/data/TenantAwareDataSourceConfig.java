package ru.aqstream.common.data;

import javax.sql.DataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;

/**
 * Конфигурация для автоматической обёртки DataSource в TenantAwareDataSourceDecorator.
 *
 * <p>Активируется свойством {@code aqstream.multitenancy.rls.enabled=true}.</p>
 *
 * <p>При активации все DataSource бины автоматически оборачиваются в
 * {@link TenantAwareDataSourceDecorator} для поддержки RLS.</p>
 *
 * @see TenantAwareDataSourceDecorator
 */
@Configuration
@ConditionalOnProperty(name = "aqstream.multitenancy.rls.enabled", havingValue = "true", matchIfMissing = false)
public class TenantAwareDataSourceConfig {

    /**
     * BeanPostProcessor для обёртки DataSource в TenantAwareDataSourceDecorator.
     */
    @Bean
    public BeanPostProcessor tenantAwareDataSourcePostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) {
                if (bean instanceof DataSource dataSource
                    && !(bean instanceof TenantAwareDataSourceDecorator)) {
                    return new TenantAwareDataSourceDecorator(dataSource);
                }
                return bean;
            }
        };
    }
}
