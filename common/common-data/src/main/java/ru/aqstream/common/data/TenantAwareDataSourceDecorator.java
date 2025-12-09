package ru.aqstream.common.data;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import ru.aqstream.common.security.TenantContext;

/**
 * Декоратор DataSource для установки tenant_id в PostgreSQL session variable.
 * Обеспечивает работу Row Level Security (RLS) политик.
 *
 * <p>При получении соединения устанавливает {@code SET app.tenant_id = 'uuid'}
 * для текущего tenant из {@link TenantContext}.</p>
 *
 * <p>ВАЖНО: Используйте этот DataSource вместо обычного для всех сервисов
 * с multi-tenancy через RLS.</p>
 *
 * @see TenantContext
 */
public class TenantAwareDataSourceDecorator implements DataSource {

    private static final org.slf4j.Logger log = LoggerFactory.getLogger(TenantAwareDataSourceDecorator.class);

    private final DataSource delegate;

    public TenantAwareDataSourceDecorator(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = delegate.getConnection();
        setTenantIdOnConnection(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        setTenantIdOnConnection(connection);
        return connection;
    }

    /**
     * Устанавливает tenant_id как session variable в PostgreSQL.
     * Если tenant_id не установлен в TenantContext, сбрасывает переменную.
     */
    private void setTenantIdOnConnection(Connection connection) throws SQLException {
        UUID tenantId = TenantContext.getTenantIdOptional().orElse(null);

        try (Statement stmt = connection.createStatement()) {
            if (tenantId != null) {
                // Устанавливаем tenant_id для RLS политик
                stmt.execute(String.format("SET app.tenant_id = '%s'", tenantId));
                log.trace("Установлен tenant_id для соединения: {}", tenantId);
            } else {
                // Сбрасываем tenant_id если контекст не установлен
                stmt.execute("RESET app.tenant_id");
                log.trace("Сброшен tenant_id для соединения");
            }
        }
    }

    // Делегирование остальных методов DataSource

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() {
        return Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface.isInstance(this) || delegate.isWrapperFor(iface);
    }
}
