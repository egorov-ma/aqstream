package ru.aqstream.common.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Контекст текущего tenant (организации) для multi-tenancy.
 * Хранит tenant_id в ThreadLocal для изоляции данных между организациями.
 *
 * <p>ВАЖНО: Контекст должен быть очищен после обработки запроса через TenantContextFilter.</p>
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
        // Утилитный класс
    }

    /**
     * Устанавливает текущий tenant.
     *
     * @param tenantId идентификатор организации
     */
    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Возвращает текущий tenant.
     *
     * @return идентификатор организации
     * @throws IllegalStateException если tenant не установлен
     */
    public static UUID getTenantId() {
        UUID tenantId = CURRENT_TENANT.get();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant context не установлен. " +
                "Убедитесь, что запрос прошёл через TenantContextFilter.");
        }
        return tenantId;
    }

    /**
     * Возвращает текущий tenant как Optional.
     *
     * @return Optional с tenant_id или пустой Optional
     */
    public static Optional<UUID> getTenantIdOptional() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    /**
     * Проверяет, установлен ли tenant context.
     *
     * @return true если tenant установлен
     */
    public static boolean isSet() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Очищает tenant context.
     * Должен вызываться в finally блоке фильтра.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
