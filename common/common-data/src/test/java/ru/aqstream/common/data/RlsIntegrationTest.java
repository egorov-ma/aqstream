package ru.aqstream.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration тесты для Row Level Security (RLS).
 * Проверяют изоляцию данных между tenant'ами на уровне PostgreSQL.
 *
 * <p>Тесты используют чистый JDBC без Spring context для проверки
 * механизма RLS независимо от JPA/Hibernate.</p>
 */
@Testcontainers
@Tag("integration")
@DisplayName("RLS Integration Tests")
class RlsIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("test_db")
        .withUsername("test")
        .withPassword("test");

    private static final UUID TENANT_A = UUID.randomUUID();
    private static final UUID TENANT_B = UUID.randomUUID();

    private static final String APP_USER = "aqstream_app";
    private static final String APP_PASSWORD = "aqstream_app";

    @BeforeAll
    static void initDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Создаём схему
            stmt.execute("CREATE SCHEMA IF NOT EXISTS event_service");

            // Создаём функцию current_tenant_id()
            stmt.execute("""
                CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS UUID AS $$
                BEGIN
                    RETURN NULLIF(current_setting('app.tenant_id', true), '')::UUID;
                EXCEPTION
                    WHEN OTHERS THEN
                        RETURN NULL;
                END;
                $$ LANGUAGE plpgsql SECURITY DEFINER STABLE
                """);

            // Создаём роль приложения (без superuser, подчиняется RLS)
            stmt.execute(String.format(
                "CREATE ROLE %s WITH LOGIN PASSWORD '%s'", APP_USER, APP_PASSWORD
            ));

            // Права на схему
            stmt.execute(String.format("GRANT USAGE ON SCHEMA event_service TO %s", APP_USER));
            stmt.execute(String.format(
                "ALTER DEFAULT PRIVILEGES IN SCHEMA event_service GRANT ALL ON TABLES TO %s", APP_USER
            ));
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Удаляем таблицу если существует
            stmt.execute("DROP TABLE IF EXISTS event_service.test_events");

            // Создаём таблицу
            stmt.execute("""
                CREATE TABLE event_service.test_events (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
                )
                """);

            // Даём права роли приложения
            stmt.execute(String.format(
                "GRANT ALL ON event_service.test_events TO %s", APP_USER
            ));

            // Включаем RLS
            stmt.execute("ALTER TABLE event_service.test_events ENABLE ROW LEVEL SECURITY");

            // Создаём политику с USING + WITH CHECK
            stmt.execute("""
                CREATE POLICY tenant_isolation ON event_service.test_events
                    FOR ALL
                    USING (tenant_id = current_tenant_id())
                    WITH CHECK (tenant_id = current_tenant_id())
                """);
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS event_service.test_events");
        }
    }

    @AfterAll
    static void cleanupDatabase() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DROP FUNCTION IF EXISTS current_tenant_id()");
            stmt.execute("DROP SCHEMA IF EXISTS event_service CASCADE");
        }
    }

    /**
     * Получает соединение с БД.
     */
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
    }

    /**
     * Получает соединение от имени роли приложения с установленным tenant_id.
     * Эта роль подчиняется RLS политикам.
     */
    private Connection getConnectionWithTenant(UUID tenantId) throws SQLException {
        Connection conn = getAppConnection();
        try (Statement stmt = conn.createStatement()) {
            if (tenantId != null) {
                stmt.execute(String.format("SET app.tenant_id = '%s'", tenantId));
            } else {
                stmt.execute("RESET app.tenant_id");
            }
        }
        return conn;
    }

    /**
     * Получает соединение от имени роли приложения (подчиняется RLS).
     */
    private static Connection getAppConnection() throws SQLException {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            APP_USER,
            APP_PASSWORD
        );
    }

    /**
     * Вставляет событие в БД с установленным tenant context.
     */
    private void insertEventDirectly(UUID tenantId, String title) throws SQLException {
        // Вставляем с установленным tenant_id для прохождения WITH CHECK
        try (Connection conn = getConnectionWithTenant(tenantId);
             PreparedStatement stmt = conn.prepareStatement(
                 "INSERT INTO event_service.test_events (id, tenant_id, title) VALUES (?, ?, ?)"
             )) {
            stmt.setObject(1, UUID.randomUUID());
            stmt.setObject(2, tenantId);
            stmt.setString(3, title);
            stmt.executeUpdate();
        }
    }

    @Nested
    @DisplayName("SELECT с RLS")
    class SelectWithRls {

        @Test
        @DisplayName("возвращает только события текущего tenant")
        void select_WithTenantContext_ReturnsOnlyOwnEvents() throws SQLException {
            // Given: события для разных tenant'ов
            insertEventDirectly(TENANT_A, "Event A1");
            insertEventDirectly(TENANT_A, "Event A2");
            insertEventDirectly(TENANT_B, "Event B1");

            // When: запрашиваем с контекстом TENANT_A
            int count;
            try (Connection conn = getConnectionWithTenant(TENANT_A);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM event_service.test_events"
                 )) {
                rs.next();
                count = rs.getInt(1);
            }

            // Then: видим только события TENANT_A
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("возвращает пустой результат для чужих данных")
        void select_DifferentTenant_ReturnsEmpty() throws SQLException {
            // Given: события только для TENANT_A
            insertEventDirectly(TENANT_A, "Event A1");
            insertEventDirectly(TENANT_A, "Event A2");

            // When: запрашиваем с контекстом TENANT_B
            int count;
            try (Connection conn = getConnectionWithTenant(TENANT_B);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM event_service.test_events"
                 )) {
                rs.next();
                count = rs.getInt(1);
            }

            // Then: не видим чужие события
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("возвращает пустой результат без tenant context")
        void select_NoTenantContext_ReturnsEmpty() throws SQLException {
            // Given: события для TENANT_A
            insertEventDirectly(TENANT_A, "Event A1");

            // When: запрашиваем БЕЗ tenant context
            int count;
            try (Connection conn = getConnectionWithTenant(null);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM event_service.test_events"
                 )) {
                rs.next();
                count = rs.getInt(1);
            }

            // Then: не видим никаких событий (current_tenant_id() = NULL)
            assertThat(count).isZero();
        }
    }

    @Nested
    @DisplayName("INSERT с RLS")
    class InsertWithRls {

        @Test
        @DisplayName("разрешает INSERT с корректным tenant_id")
        void insert_MatchingTenantId_Succeeds() throws SQLException {
            // When: вставляем событие с tenant_id = TENANT_A при контексте TENANT_A
            try (Connection conn = getConnectionWithTenant(TENANT_A);
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO event_service.test_events (id, tenant_id, title) VALUES (?, ?, ?)"
                 )) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, TENANT_A);  // Совпадает с context
                stmt.setString(3, "New Event");

                int inserted = stmt.executeUpdate();

                // Then: вставка успешна
                assertThat(inserted).isEqualTo(1);
            }
        }

        @Test
        @DisplayName("блокирует INSERT с чужим tenant_id")
        void insert_DifferentTenantId_Fails() throws SQLException {
            // When: пытаемся вставить событие с tenant_id = TENANT_B при контексте TENANT_A
            try (Connection conn = getConnectionWithTenant(TENANT_A);
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO event_service.test_events (id, tenant_id, title) VALUES (?, ?, ?)"
                 )) {
                stmt.setObject(1, UUID.randomUUID());
                stmt.setObject(2, TENANT_B);  // НЕ совпадает с context
                stmt.setString(3, "Malicious Event");

                // Then: RLS блокирует вставку
                assertThrows(SQLException.class, stmt::executeUpdate);
            }
        }
    }

    @Nested
    @DisplayName("UPDATE с RLS")
    class UpdateWithRls {

        @Test
        @DisplayName("разрешает UPDATE только своих данных")
        void update_OwnData_Succeeds() throws SQLException {
            // Given: событие для TENANT_A
            insertEventDirectly(TENANT_A, "Original Title");

            // When: обновляем с контекстом TENANT_A
            int updated;
            try (Connection conn = getConnectionWithTenant(TENANT_A);
                 Statement stmt = conn.createStatement()) {
                updated = stmt.executeUpdate(
                    "UPDATE event_service.test_events SET title = 'Updated Title'"
                );
            }

            // Then: обновление успешно
            assertThat(updated).isEqualTo(1);
        }

        @Test
        @DisplayName("не обновляет чужие данные (возвращает 0)")
        void update_OtherTenantData_AffectsNothing() throws SQLException {
            // Given: событие для TENANT_A
            insertEventDirectly(TENANT_A, "Original Title");

            // When: пытаемся обновить с контекстом TENANT_B
            int updated;
            try (Connection conn = getConnectionWithTenant(TENANT_B);
                 Statement stmt = conn.createStatement()) {
                updated = stmt.executeUpdate(
                    "UPDATE event_service.test_events SET title = 'Hacked Title'"
                );
            }

            // Then: ничего не обновлено (RLS фильтрует)
            assertThat(updated).isZero();
        }
    }

    @Nested
    @DisplayName("DELETE с RLS")
    class DeleteWithRls {

        @Test
        @DisplayName("разрешает DELETE только своих данных")
        void delete_OwnData_Succeeds() throws SQLException {
            // Given: событие для TENANT_A
            insertEventDirectly(TENANT_A, "To Delete");

            // When: удаляем с контекстом TENANT_A
            int deleted;
            try (Connection conn = getConnectionWithTenant(TENANT_A);
                 Statement stmt = conn.createStatement()) {
                deleted = stmt.executeUpdate(
                    "DELETE FROM event_service.test_events"
                );
            }

            // Then: удаление успешно
            assertThat(deleted).isEqualTo(1);
        }

        @Test
        @DisplayName("не удаляет чужие данные (возвращает 0)")
        void delete_OtherTenantData_AffectsNothing() throws SQLException {
            // Given: событие для TENANT_A
            insertEventDirectly(TENANT_A, "Protected");

            // When: пытаемся удалить с контекстом TENANT_B
            int deleted;
            try (Connection conn = getConnectionWithTenant(TENANT_B);
                 Statement stmt = conn.createStatement()) {
                deleted = stmt.executeUpdate(
                    "DELETE FROM event_service.test_events"
                );
            }

            // Then: ничего не удалено (RLS фильтрует)
            assertThat(deleted).isZero();

            // Проверяем, что данные всё ещё существуют
            try (Connection conn = getConnectionWithTenant(TENANT_A);
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM event_service.test_events"
                 )) {
                rs.next();
                assertThat(rs.getInt(1)).isEqualTo(1);
            }
        }
    }

    @Nested
    @DisplayName("Переключение tenant context")
    class TenantContextSwitch {

        @Test
        @DisplayName("корректно переключает контекст в рамках одного соединения")
        void switchTenant_SameConnection_SeesCorrectData() throws SQLException {
            // Given: события для разных tenant'ов
            insertEventDirectly(TENANT_A, "Event A");
            insertEventDirectly(TENANT_B, "Event B");

            try (Connection conn = getAppConnection()) {
                // Сначала как TENANT_A
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(String.format("SET app.tenant_id = '%s'", TENANT_A));
                }

                int countA;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM event_service.test_events"
                     )) {
                    rs.next();
                    countA = rs.getInt(1);
                }
                assertThat(countA).isEqualTo(1);

                // Переключаемся на TENANT_B
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(String.format("SET app.tenant_id = '%s'", TENANT_B));
                }

                int countB;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM event_service.test_events"
                     )) {
                    rs.next();
                    countB = rs.getInt(1);
                }
                assertThat(countB).isEqualTo(1);

                // Сбрасываем контекст
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute("RESET app.tenant_id");
                }

                int countNone;
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT COUNT(*) FROM event_service.test_events"
                     )) {
                    rs.next();
                    countNone = rs.getInt(1);
                }
                assertThat(countNone).isZero();
            }
        }
    }
}
