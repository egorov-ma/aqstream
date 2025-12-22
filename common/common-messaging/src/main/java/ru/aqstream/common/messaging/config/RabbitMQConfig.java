package ru.aqstream.common.messaging.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Базовая конфигурация RabbitMQ для всех сервисов.
 *
 * <p>Определяет exchanges, queues, bindings и настройки сериализации.
 * Exchanges и queues также определены в definitions.json для автоматического
 * создания при старте RabbitMQ.</p>
 *
 * <p><b>ВАЖНО:</b> Source of truth для топологии RabbitMQ — файл
 * {@code docker/rabbitmq/definitions.json}. При изменении exchanges, queues
 * или bindings обновляйте оба места для синхронизации.</p>
 *
 * <p>Для активации в сервисе импортируйте эту конфигурацию:</p>
 * <pre>
 * {@code
 * @Import(RabbitMQConfig.class)
 * @Configuration
 * public class MyServiceConfig { }
 * }
 * </pre>
 */
@Configuration
public class RabbitMQConfig {

    /**
     * Название основного exchange для доменных событий.
     */
    public static final String EVENTS_EXCHANGE = "aqstream.events";

    /**
     * Название Dead Letter Exchange для failed messages.
     */
    public static final String DLX_EXCHANGE = "aqstream.events.dlx";

    /**
     * Название exchange для уведомлений (direct routing).
     */
    public static final String NOTIFICATIONS_EXCHANGE = "aqstream.notifications";

    /**
     * Очередь для Notification Service.
     * Получает события: notification.#, registration.#, event.#
     */
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    /**
     * Очередь для Analytics Service.
     * Получает все события (#).
     */
    public static final String ANALYTICS_QUEUE = "analytics.queue";

    /**
     * Очередь для Event Service.
     * Получает события: payment.#
     */
    public static final String EVENT_SERVICE_QUEUE = "event-service.queue";

    /**
     * Очередь для Dead Letter messages.
     */
    public static final String DLX_QUEUE = "dlx.queue";

    // === Exchanges ===

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(DLX_EXCHANGE, true, false);
    }

    // === Queues ===

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "notification.dlx")
            .build();
    }

    @Bean
    public Queue analyticsQueue() {
        return QueueBuilder.durable(ANALYTICS_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "analytics.dlx")
            .build();
    }

    @Bean
    public Queue eventServiceQueue() {
        return QueueBuilder.durable(EVENT_SERVICE_QUEUE)
            .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
            .withArgument("x-dead-letter-routing-key", "event-service.dlx")
            .build();
    }

    @Bean
    public Queue dlxQueue() {
        return QueueBuilder.durable(DLX_QUEUE).build();
    }

    // === Bindings ===

    @Bean
    public Binding notificationBindingNotification(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with("notification.#");
    }

    @Bean
    public Binding notificationBindingRegistration(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with("registration.#");
    }

    @Bean
    public Binding notificationBindingEvent(Queue notificationQueue, TopicExchange eventsExchange) {
        return BindingBuilder.bind(notificationQueue).to(eventsExchange).with("event.#");
    }

    @Bean
    public Binding analyticsBinding(Queue analyticsQueue, TopicExchange eventsExchange) {
        // Analytics слушает все события для полной картины
        return BindingBuilder.bind(analyticsQueue).to(eventsExchange).with("#");
    }

    @Bean
    public Binding eventServiceBindingPayment(Queue eventServiceQueue, TopicExchange eventsExchange) {
        // Event Service слушает события платежей
        return BindingBuilder.bind(eventServiceQueue).to(eventsExchange).with("payment.#");
    }

    @Bean
    public Binding eventServiceBindingOrganization(Queue eventServiceQueue, TopicExchange eventsExchange) {
        // Event Service слушает события организаций (для архивирования при удалении)
        return BindingBuilder.bind(eventServiceQueue).to(eventsExchange).with("organization.#");
    }

    @Bean
    public Binding dlxBinding(Queue dlxQueue, TopicExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with("#");
    }

    // === Message Converter ===

    /**
     * JSON конвертер для сериализации/десериализации сообщений.
     * Использует Jackson ObjectMapper из Spring контекста.
     *
     * @param objectMapper Jackson ObjectMapper
     * @return конвертер сообщений
     */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    // === RabbitTemplate ===

    /**
     * Настроенный RabbitTemplate с JSON конвертером.
     * Использует основной exchange для отправки событий.
     *
     * @param connectionFactory фабрика соединений
     * @param messageConverter  конвертер сообщений
     * @return настроенный RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        template.setExchange(EVENTS_EXCHANGE);
        return template;
    }

    // === Listener Container Factory ===

    /**
     * Фабрика контейнеров для слушателей RabbitMQ.
     * Настраивает JSON конвертер и поведение при ошибках.
     *
     * @param connectionFactory фабрика соединений
     * @param messageConverter  конвертер сообщений
     * @return настроенная фабрика
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
        ConnectionFactory connectionFactory,
        MessageConverter messageConverter) {

        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        // При ошибке не возвращать в очередь - отправлять в DLQ
        factory.setDefaultRequeueRejected(false);
        // Количество сообщений для предварительной загрузки
        factory.setPrefetchCount(10);
        return factory;
    }
}
