package ru.aqstream.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.util.UUID;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Базовая сущность с поддержкой multi-tenancy.
 * Все бизнес-сущности, изолированные по организациям, должны наследоваться от этого класса.
 *
 * <p>tenant_id устанавливается автоматически через {@link TenantEntityListener}.</p>
 */
@MappedSuperclass
@EntityListeners({AuditingEntityListener.class, TenantEntityListener.class})
public abstract class TenantAwareEntity extends BaseEntity {

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    /**
     * Идентификатор организации (tenant).
     *
     * @return UUID организации
     */
    public UUID getTenantId() {
        return tenantId;
    }

    /**
     * Устанавливает идентификатор организации.
     * Обычно вызывается автоматически через TenantEntityListener.
     *
     * @param tenantId идентификатор организации
     */
    public void setTenantId(UUID tenantId) {
        this.tenantId = tenantId;
    }
}
