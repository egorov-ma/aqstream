package ru.aqstream.common.data;

import jakarta.persistence.PrePersist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.aqstream.common.security.TenantContext;

/**
 * JPA Entity Listener для автоматического заполнения tenant_id.
 * Устанавливает tenant_id из TenantContext при создании новой сущности.
 */
public class TenantEntityListener {

    private static final Logger log = LoggerFactory.getLogger(TenantEntityListener.class);

    /**
     * Устанавливает tenant_id перед сохранением новой сущности.
     *
     * @param entity сущность для сохранения
     */
    @PrePersist
    public void setTenantId(TenantAwareEntity entity) {
        if (entity.getTenantId() != null) {
            // tenant_id уже установлен (например, в тестах)
            return;
        }

        if (!TenantContext.isSet()) {
            log.warn("TenantContext не установлен при сохранении сущности: {}",
                entity.getClass().getSimpleName());
            throw new IllegalStateException(
                "Невозможно сохранить сущность без tenant context. " +
                "Убедитесь, что запрос прошёл через TenantContextFilter."
            );
        }

        entity.setTenantId(TenantContext.getTenantId());
    }
}
