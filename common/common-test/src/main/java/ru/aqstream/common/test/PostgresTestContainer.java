package ru.aqstream.common.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Singleton Testcontainer для PostgreSQL.
 * Наследуйте этот класс в интеграционных тестах для автоматической настройки БД.
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @IntegrationTest}
 * class EventRepositoryTest extends PostgresTestContainer {
 *
 *     @Autowired
 *     private EventRepository repository;
 *
 *     @Test
 *     void findById_ExistingEvent_ReturnsEvent() {
 *         // ...
 *     }
 * }
 * </pre>
 *
 * <p>Контейнер запускается один раз и переиспользуется всеми тестами,
 * что значительно ускоряет выполнение тестов.</p>
 */
public abstract class PostgresTestContainer {

    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_db")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true); // Переиспользование между запусками

        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // JPA settings for tests
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.show-sql", () -> "false");
    }

    /**
     * Возвращает JDBC URL для текущего контейнера.
     *
     * @return JDBC URL
     */
    protected static String getJdbcUrl() {
        return POSTGRES.getJdbcUrl();
    }

    /**
     * Возвращает host контейнера.
     *
     * @return host
     */
    protected static String getHost() {
        return POSTGRES.getHost();
    }

    /**
     * Возвращает mapped порт контейнера.
     *
     * @return порт
     */
    protected static Integer getPort() {
        return POSTGRES.getFirstMappedPort();
    }
}
