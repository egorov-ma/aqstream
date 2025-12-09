package ru.aqstream.common.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.aqstream.common.security.TenantContext;

/**
 * Unit тесты для TenantAwareDataSourceDecorator.
 * Проверяют, что session variable устанавливается корректно.
 */
class TenantAwareDataSourceDecoratorTest {

    private DataSource delegate;
    private Connection connection;
    private Statement statement;
    private TenantAwareDataSourceDecorator decorator;

    @BeforeEach
    void setUp() throws Exception {
        delegate = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(Statement.class);

        when(delegate.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        decorator = new TenantAwareDataSourceDecorator(delegate);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("При наличии TenantContext устанавливается app.tenant_id")
    void getConnection_WithTenantContext_SetsTenantId() throws Exception {
        // Given
        UUID tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId);

        // When
        Connection result = decorator.getConnection();

        // Then
        assertThat(result).isEqualTo(connection);
        verify(statement).execute(String.format("SET app.tenant_id = '%s'", tenantId));
    }

    @Test
    @DisplayName("Без TenantContext сбрасывается app.tenant_id")
    void getConnection_WithoutTenantContext_ResetsTenantId() throws Exception {
        // Given: TenantContext не установлен

        // When
        Connection result = decorator.getConnection();

        // Then
        assertThat(result).isEqualTo(connection);
        verify(statement).execute("RESET app.tenant_id");
    }

    @Test
    @DisplayName("Декоратор делегирует getConnection(username, password)")
    void getConnection_WithCredentials_DelegatesToDelegate() throws Exception {
        // Given
        String username = "user";
        String password = "pass";
        when(delegate.getConnection(username, password)).thenReturn(connection);

        // When
        Connection result = decorator.getConnection(username, password);

        // Then
        assertThat(result).isEqualTo(connection);
        verify(delegate).getConnection(username, password);
    }
}
