package ru.aqstream.common.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Контекст текущего пользователя для RLS.
 * Хранит user_id в ThreadLocal для проверки доступа к пользовательским данным.
 *
 * <p>Используется для RLS политик, которые позволяют пользователям
 * читать свои собственные данные (например, регистрации) независимо от tenant.</p>
 *
 * <p>ВАЖНО: Контекст должен быть очищен после обработки запроса.</p>
 */
public final class UserContext {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
        // Утилитный класс
    }

    /**
     * Устанавливает текущего пользователя.
     *
     * @param userId идентификатор пользователя
     */
    public static void setUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    /**
     * Возвращает текущего пользователя.
     *
     * @return идентификатор пользователя
     * @throws IllegalStateException если пользователь не установлен
     */
    public static UUID getUserId() {
        UUID userId = CURRENT_USER.get();
        if (userId == null) {
            throw new IllegalStateException("User context не установлен. " +
                "Убедитесь, что запрос прошёл через TenantContextFilter.");
        }
        return userId;
    }

    /**
     * Возвращает текущего пользователя как Optional.
     *
     * @return Optional с user_id или пустой Optional
     */
    public static Optional<UUID> getUserIdOptional() {
        return Optional.ofNullable(CURRENT_USER.get());
    }

    /**
     * Проверяет, установлен ли user context.
     *
     * @return true если пользователь установлен
     */
    public static boolean isSet() {
        return CURRENT_USER.get() != null;
    }

    /**
     * Очищает user context.
     * Должен вызываться в finally блоке фильтра.
     */
    public static void clear() {
        CURRENT_USER.remove();
    }
}
