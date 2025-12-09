package ru.aqstream.common.test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserPrincipal;

/**
 * Утилитный класс для создания тестовых данных.
 *
 * <p>Предоставляет фабричные методы для создания тестовых объектов:</p>
 * <ul>
 *   <li>UserPrincipal с предопределёнными ролями</li>
 *   <li>UUID для tenant, user и других сущностей</li>
 *   <li>Instant для дат в прошлом/будущем</li>
 * </ul>
 */
public final class TestFixtures {

    // Предопределённые UUID для консистентных тестов
    public static final UUID TEST_TENANT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID TEST_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final UUID TEST_ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");
    public static final UUID TEST_ORGANIZER_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");

    public static final String TEST_USER_EMAIL = "user@test.aqstream.ru";
    public static final String TEST_ADMIN_EMAIL = "admin@test.aqstream.ru";
    public static final String TEST_ORGANIZER_EMAIL = "organizer@test.aqstream.ru";

    private TestFixtures() {
        // Утилитный класс
    }

    // === UserPrincipal Factories ===

    /**
     * Создаёт тестового пользователя с ролью USER.
     *
     * @return UserPrincipal с ролью USER
     */
    public static UserPrincipal testUser() {
        return new UserPrincipal(TEST_USER_ID, TEST_USER_EMAIL, TEST_TENANT_ID, Set.of("USER"));
    }

    /**
     * Создаёт тестового администратора.
     *
     * @return UserPrincipal с ролью ADMIN
     */
    public static UserPrincipal testAdmin() {
        return new UserPrincipal(TEST_ADMIN_ID, TEST_ADMIN_EMAIL, TEST_TENANT_ID, Set.of("ADMIN", "USER"));
    }

    /**
     * Создаёт тестового организатора.
     *
     * @return UserPrincipal с ролью ORGANIZER
     */
    public static UserPrincipal testOrganizer() {
        return new UserPrincipal(TEST_ORGANIZER_ID, TEST_ORGANIZER_EMAIL, TEST_TENANT_ID, Set.of("ORGANIZER", "USER"));
    }

    /**
     * Создаёт пользователя с произвольными данными.
     *
     * @param userId   ID пользователя
     * @param tenantId ID организации
     * @param roles    роли
     * @return UserPrincipal
     */
    public static UserPrincipal userWithRoles(UUID userId, UUID tenantId, String... roles) {
        return new UserPrincipal(userId, "test-" + userId + "@test.aqstream.ru", tenantId, Set.of(roles));
    }

    // === TenantContext Setup ===

    /**
     * Устанавливает тестовый TenantContext.
     * Используйте в try-finally блоке.
     *
     * <pre>
     * TestFixtures.setupTenantContext();
     * try {
     *     // тест
     * } finally {
     *     TenantContext.clear();
     * }
     * </pre>
     */
    public static void setupTenantContext() {
        TenantContext.setTenantId(TEST_TENANT_ID);
    }

    /**
     * Устанавливает произвольный TenantContext.
     *
     * @param tenantId ID организации
     */
    public static void setupTenantContext(UUID tenantId) {
        TenantContext.setTenantId(tenantId);
    }

    // === Date/Time Factories ===

    /**
     * Возвращает момент времени в будущем.
     *
     * @param days количество дней
     * @return Instant в будущем
     */
    public static Instant futureInstant(int days) {
        return Instant.now().plus(days, ChronoUnit.DAYS);
    }

    /**
     * Возвращает момент времени в прошлом.
     *
     * @param days количество дней
     * @return Instant в прошлом
     */
    public static Instant pastInstant(int days) {
        return Instant.now().minus(days, ChronoUnit.DAYS);
    }

    /**
     * Возвращает момент времени через указанное количество часов.
     *
     * @param hours количество часов (положительное — будущее, отрицательное — прошлое)
     * @return Instant
     */
    public static Instant hoursFromNow(int hours) {
        return Instant.now().plus(hours, ChronoUnit.HOURS);
    }

    // === UUID Factories ===

    /**
     * Генерирует случайный UUID.
     *
     * @return новый UUID
     */
    public static UUID randomId() {
        return UUID.randomUUID();
    }

    /**
     * Создаёт UUID из числа (для читаемости в тестах).
     *
     * @param number число от 1 до 999999
     * @return UUID вида 00000000-0000-0000-0000-000000000001
     */
    public static UUID numberedId(int number) {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", number));
    }
}
