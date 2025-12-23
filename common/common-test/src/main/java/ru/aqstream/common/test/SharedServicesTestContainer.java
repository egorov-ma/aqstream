package ru.aqstream.common.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Singleton Testcontainer для shared_services_db с поддержкой RLS.
 * Используйте для интеграционных тестов сервисов, работающих с shared БД
 * (event-service, notification-service, media-service).
 *
 * <p>Контейнер инициализируется с:</p>
 * <ul>
 *   <li>Функцией {@code current_tenant_id()} для RLS политик</li>
 *   <li>Схемами: event_service, notification_service, media_service</li>
 *   <li>Ролью aqstream_app без superuser привилегий</li>
 * </ul>
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @IntegrationTest}
 * class EventRepositoryRlsTest extends SharedServicesTestContainer {
 *
 *     @Autowired
 *     private EventRepository repository;
 *
 *     @Test
 *     void find_DifferentTenant_ReturnsEmpty() {
 *         // ...
 *     }
 * }
 * </pre>
 */
public abstract class SharedServicesTestContainer {

    /**
     * Защищённый конструктор для наследников.
     */
    protected SharedServicesTestContainer() {
        // Пустой конструктор для наследования
    }

    @SuppressWarnings("resource") // Singleton контейнер переиспользуется между тестами
    private static final PostgreSQLContainer<?> POSTGRES;

    @SuppressWarnings("resource") // Singleton контейнер переиспользуется между тестами
    private static final RabbitMQContainer RABBITMQ;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("shared_services_db")
            .withUsername("aqstream")
            .withPassword("aqstream")
            .withInitScript("init-shared-services.sql")
            .withReuse(true); // Переиспользование между запусками

        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withReuse(true);

        POSTGRES.start();
        RABBITMQ.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // PostgreSQL
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        // Liquibase управляет схемой, НЕ Hibernate
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");

        // Включаем RLS
        registry.add("aqstream.multitenancy.rls.enabled", () -> "true");

        // RabbitMQ
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
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
