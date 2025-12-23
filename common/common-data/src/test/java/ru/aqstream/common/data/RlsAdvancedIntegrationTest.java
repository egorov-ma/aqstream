package ru.aqstream.common.data;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Дополнительные integration тесты для Row Level Security (RLS).
 * Проверяют superuser bypass и переключение tenant context.
 */
@Testcontainers
@Tag("integration")
@DisplayName("RLS Advanced Integration Tests")
class RlsAdvancedIntegrationTest {

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

            stmt.execute("CREATE SCHEMA IF NOT EXISTS event_service");

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

            stmt.execute(String.format(
                "CREATE ROLE %s WITH LOGIN PASSWORD '%s'", APP_USER, APP_PASSWORD
            ));

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

            stmt.execute("DROP TABLE IF EXISTS event_service.test_events");

            stmt.execute("""
                CREATE TABLE event_service.test_events (
                    id UUID PRIMARY KEY,
                    tenant_id UUID NOT NULL,
                    title VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
                )
                """);

            stmt.execute(String.format(
                "GRANT ALL ON event_service.test_events TO %s", APP_USER
            ));

            stmt.execute("ALTER TABLE event_service.test_events ENABLE ROW LEVEL SECURITY");

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

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            POSTGRES.getUsername(),
            POSTGRES.getPassword()
        );
    }

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

    private static Connection getAppConnection() throws SQLException {
        return DriverManager.getConnection(
            POSTGRES.getJdbcUrl(),
            APP_USER,
            APP_PASSWORD
        );
    }

    private void insertEventDirectly(UUID tenantId, String title) throws SQLException {
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
    @DisplayName("Superuser bypass RLS")
    @Tag("e2e")
    class SuperuserBypassRls {

        @Test
        @DisplayName("superuser видит данные всех tenant'ов (без RLS)")
        void select_Superuser_SeesAllData() throws SQLException {
            insertEventDirectly(TENANT_A, "Event A");
            insertEventDirectly(TENANT_B, "Event B");

            int count;
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM event_service.test_events"
                 )) {
                rs.next();
                count = rs.getInt(1);
            }

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("superuser может читать данные любого tenant")
        void select_Superuser_CanReadAnyTenant() throws SQLException {
            insertEventDirectly(TENANT_A, "Secret Event A1");
            insertEventDirectly(TENANT_A, "Secret Event A2");

            int count;
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM event_service.test_events WHERE tenant_id = '" + TENANT_A + "'"
                 )) {
                rs.next();
                count = rs.getInt(1);
            }

            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Переключение tenant context")
    class TenantContextSwitch {

        @Test
        @DisplayName("корректно переключает контекст в рамках одного соединения")
        void switchTenant_SameConnection_SeesCorrectData() throws SQLException {
            insertEventDirectly(TENANT_A, "Event A");
            insertEventDirectly(TENANT_B, "Event B");

            try (Connection conn = getAppConnection()) {
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
