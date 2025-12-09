package ru.aqstream.common.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;

/**
 * Singleton Testcontainer для RabbitMQ.
 * Наследуйте этот класс в интеграционных тестах, требующих message broker.
 *
 * <p>Использование:</p>
 * <pre>
 * {@code @IntegrationTest}
 * class EventPublisherTest extends RabbitMQTestContainer {
 *
 *     @Autowired
 *     private EventPublisher publisher;
 *
 *     @Test
 *     void publish_ValidEvent_SendsToQueue() {
 *         // ...
 *     }
 * }
 * </pre>
 */
public abstract class RabbitMQTestContainer {

    private static final RabbitMQContainer RABBITMQ;

    static {
        RABBITMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine")
            .withReuse(true);

        RABBITMQ.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", RABBITMQ::getHost);
        registry.add("spring.rabbitmq.port", RABBITMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
    }

    /**
     * Возвращает host контейнера.
     *
     * @return host
     */
    protected static String getHost() {
        return RABBITMQ.getHost();
    }

    /**
     * Возвращает AMQP порт контейнера.
     *
     * @return AMQP порт
     */
    protected static Integer getAmqpPort() {
        return RABBITMQ.getAmqpPort();
    }

    /**
     * Возвращает HTTP порт management UI.
     *
     * @return HTTP порт
     */
    protected static Integer getHttpPort() {
        return RABBITMQ.getHttpPort();
    }
}
