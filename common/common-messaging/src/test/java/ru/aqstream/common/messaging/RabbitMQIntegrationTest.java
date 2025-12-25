package ru.aqstream.common.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import net.datafaker.Faker;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.aqstream.common.messaging.config.RabbitMQConfig;

/**
 * Интеграционные тесты для RabbitMQ с использованием Testcontainers.
 * Проверяют отправку и получение сообщений через RabbitMQ.
 */
@SpringBootTest(
    classes = RabbitMQIntegrationTest.TestConfig.class,
    properties = "spring.main.allow-bean-definition-overriding=true"
)
@Testcontainers
class RabbitMQIntegrationTest {

    private static final Faker FAKER = new Faker();

    @Container
    static RabbitMQContainer rabbitMQ = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.rabbitmq.host", rabbitMQ::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQ::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitMQ::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitMQ::getAdminPassword);
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private TestMessageListener testMessageListener;

    @Test
    @DisplayName("Сообщение успешно отправляется и получается через notification.queue")
    void sendAndReceive_NotificationQueue_Success() throws Exception {
        // Given
        String eventType = "event.created";
        String testTitle = FAKER.book().title();
        String payload = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"title\":\"" + testTitle + "\"}";

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTS_EXCHANGE, eventType, payload);

        // Then
        boolean received = testMessageListener.awaitMessage(5, TimeUnit.SECONDS);
        assertThat(received).isTrue();
        assertThat(testMessageListener.getLastMessage()).contains(testTitle);
    }

    @Test
    @DisplayName("Сообщения маршрутизируются в analytics.queue по wildcard #")
    void sendAndReceive_AnalyticsQueue_ReceivesAllEvents() throws Exception {
        // Given
        String eventType = "user.registered";
        String payload = "{\"userId\":\"" + UUID.randomUUID() + "\"}";

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTS_EXCHANGE, eventType, payload);

        // Then - analytics queue получает все события
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                assertThat(testMessageListener.getAnalyticsMessageCount()).isGreaterThan(0);
            });
    }

    @Test
    @DisplayName("Сообщения payment.* маршрутизируются в event-service.queue")
    void sendAndReceive_EventServiceQueue_ReceivesPaymentEvents() throws Exception {
        // Given
        String eventType = "payment.completed";
        String payload = "{\"paymentId\":\"" + UUID.randomUUID() + "\",\"amount\":100}";

        // When
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENTS_EXCHANGE, eventType, payload);

        // Then
        await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> {
                assertThat(testMessageListener.getEventServiceMessageCount()).isGreaterThan(0);
            });
    }

    @Test
    @DisplayName("RabbitTemplate использует правильный exchange по умолчанию")
    void rabbitTemplate_DefaultExchange_IsEventsExchange() {
        assertThat(rabbitTemplate.getExchange()).isEqualTo(RabbitMQConfig.EVENTS_EXCHANGE);
    }

    @Configuration
    @EnableAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        ru.aqstream.common.messaging.config.MessagingAutoConfiguration.class
    })
    @Import(RabbitMQConfig.class)
    static class TestConfig {

        @Bean
        public ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        public TestMessageListener testMessageListener() {
            return new TestMessageListener();
        }
    }

    /**
     * Тестовый слушатель для проверки получения сообщений.
     */
    static class TestMessageListener {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final AtomicReference<String> lastMessage = new AtomicReference<>();
        private final AtomicInteger analyticsMessageCount = new AtomicInteger(0);
        private final AtomicInteger eventServiceMessageCount = new AtomicInteger(0);

        @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
        public void handleNotification(Message message) {
            lastMessage.set(new String(message.getBody()));
            latch.countDown();
        }

        @RabbitListener(queues = RabbitMQConfig.ANALYTICS_QUEUE)
        public void handleAnalytics(Message message) {
            analyticsMessageCount.incrementAndGet();
        }

        @RabbitListener(queues = RabbitMQConfig.EVENT_SERVICE_QUEUE)
        public void handleEventService(Message message) {
            eventServiceMessageCount.incrementAndGet();
        }

        public boolean awaitMessage(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        public String getLastMessage() {
            return lastMessage.get();
        }

        public int getAnalyticsMessageCount() {
            return analyticsMessageCount.get();
        }

        public int getEventServiceMessageCount() {
            return eventServiceMessageCount.get();
        }
    }
}
