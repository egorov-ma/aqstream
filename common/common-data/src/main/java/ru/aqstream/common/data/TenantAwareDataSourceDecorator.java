package ru.aqstream.common.data;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.common.security.UserContext;

/**
 * Декоратор DataSource для установки session variables в PostgreSQL.
 * Обеспечивает работу Row Level Security (RLS) политик.
 *
 * <p>При получении соединения устанавливает:</p>
 * <ul>
 *   <li>{@code app.tenant_id} — для изоляции данных между организациями</li>
 *   <li>{@code app.user_id} — для доступа пользователей к своим данным</li>
 * </ul>
 *
 * <p>ВАЖНО: Используйте этот DataSource вместо обычного для всех сервисов
 * с multi-tenancy через RLS.</p>
 *
 * @see TenantContext
 * @see UserContext
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
        try {
            setSessionVariables(connection);
            return connection;
        } catch (SQLException e) {
            // Закрываем соединение при ошибке установки переменных
            closeQuietly(connection);
            throw e;
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = delegate.getConnection(username, password);
        try {
            setSessionVariables(connection);
            return connection;
        } catch (SQLException e) {
            closeQuietly(connection);
            throw e;
        }
    }

    /**
     * Закрывает соединение без выброса исключения.
     */
    private void closeQuietly(Connection connection) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.warn("Ошибка при закрытии соединения: {}", e.getMessage());
        }
    }

    /**
     * Устанавливает session variables в PostgreSQL для RLS.
     * Устанавливает app.tenant_id и app.user_id из соответствующих контекстов.
     *
     * <p>Используем PreparedStatement с set_config() для безопасной установки
     * параметров сессии без риска SQL injection.</p>
     */
    private void setSessionVariables(Connection connection) throws SQLException {
        UUID tenantId = TenantContext.getTenantIdOptional().orElse(null);
        UUID userId = UserContext.getUserIdOptional().orElse(null);

        // Устанавливаем tenant_id
        if (tenantId != null) {
            try (var stmt = connection.prepareStatement("SELECT set_config('app.tenant_id', ?, false)")) {
                stmt.setString(1, tenantId.toString());
                stmt.execute();
            }
        } else {
            try (var stmt = connection.createStatement()) {
                stmt.execute("RESET app.tenant_id");
            }
        }

        // Устанавливаем user_id для RLS политик пользовательских данных
        if (userId != null) {
            try (var stmt = connection.prepareStatement("SELECT set_config('app.user_id', ?, false)")) {
                stmt.setString(1, userId.toString());
                stmt.execute();
            }
        } else {
            try (var stmt = connection.createStatement()) {
                stmt.execute("RESET app.user_id");
            }
        }

        log.trace("Установлены session variables: tenant_id={}, user_id={}", tenantId, userId);
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
