package ru.aqstream.notification.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.aqstream.event.api.event.EventCancelledEvent;
import ru.aqstream.event.api.event.RegistrationCancelledEvent;
import ru.aqstream.event.api.event.RegistrationCreatedEvent;
import ru.aqstream.notification.config.NotificationProperties;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.service.NotificationService;
import ru.aqstream.user.api.event.EmailVerificationRequestedEvent;
import ru.aqstream.user.api.event.OrganizationRequestApprovedEvent;
import ru.aqstream.user.api.event.OrganizationRequestRejectedEvent;
import ru.aqstream.user.api.event.PasswordResetRequestedEvent;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Слушатель событий RabbitMQ для отправки уведомлений.
 *
 * <p>Обрабатывает события из очереди notification.queue и отправляет
 * соответствующие уведомления через Telegram и Email.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private static final String NOTIFICATION_QUEUE = "notification.queue";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")
            .withZone(ZoneId.of("Europe/Moscow"));

    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;

    // === Registration Events ===

    /**
     * Обрабатывает создание регистрации.
     * Отправляет билет с QR-кодом участнику.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "registration-created")
    public void handleRegistrationCreated(RegistrationCreatedEvent event) {
        log.info("Получено событие RegistrationCreatedEvent: registrationId={}, userId={}",
            event.getRegistrationId(), event.getUserId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("firstName", event.getFirstName());
            variables.put("eventTitle", event.getEventTitle());
            variables.put("confirmationCode", event.getConfirmationCode());
            variables.put("ticketTypeName", event.getTicketTypeName());
            variables.put("eventDate", formatDate(event.getEventStartsAt()));
            variables.put("eventUrl", buildEventUrl(event.getEventSlug()));

            notificationService.sendTelegram(
                event.getUserId(),
                "registration.confirmed",
                variables,
                NotificationPreference.REGISTRATION_UPDATES
            );
        } catch (Exception e) {
            log.error("Ошибка обработки RegistrationCreatedEvent: registrationId={}, error={}",
                event.getRegistrationId(), e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает отмену регистрации.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "registration-cancelled")
    public void handleRegistrationCancelled(RegistrationCancelledEvent event) {
        log.info("Получено событие RegistrationCancelledEvent: registrationId={}, userId={}",
            event.getRegistrationId(), event.getUserId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("firstName", event.getFirstName());
            variables.put("eventTitle", event.getEventTitle());
            variables.put("eventDate", formatDate(event.getEventStartsAt()));
            variables.put("cancelledByOrganizer", event.isCancelledByOrganizer());
            if (event.getCancellationReason() != null) {
                variables.put("cancellationReason", event.getCancellationReason());
            }

            notificationService.sendTelegram(
                event.getUserId(),
                "registration.cancelled",
                variables,
                NotificationPreference.REGISTRATION_UPDATES
            );
        } catch (Exception e) {
            log.error("Ошибка обработки RegistrationCancelledEvent: registrationId={}, error={}",
                event.getRegistrationId(), e.getMessage(), e);
        }
    }

    // === Event Events ===

    /**
     * Обрабатывает отмену события.
     * Уведомление всем участникам отправляется через отдельный batch процесс.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "event-cancelled")
    public void handleEventCancelled(EventCancelledEvent event) {
        log.info("Получено событие EventCancelledEvent: eventId={}", event.getEventId());

        // TODO: Реализовать массовую отправку уведомлений всем участникам
        // Требуется получить список registrations через EventClient
        // и отправить уведомление каждому участнику с rate limiting
        log.warn("Массовая рассылка при отмене события не реализована: eventId={}", event.getEventId());
    }

    // === Organization Request Events ===

    /**
     * Обрабатывает одобрение запроса на организацию.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "organization-request-approved")
    public void handleOrganizationRequestApproved(OrganizationRequestApprovedEvent event) {
        log.info("Получено событие OrganizationRequestApprovedEvent: requestId={}, userId={}",
            event.getRequestId(), event.getUserId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("organizationName", event.getOrganizationName());
            variables.put("organizationUrl", buildOrganizationUrl(event.getSlug()));

            notificationService.sendTelegram(
                event.getUserId(),
                "organization.request.approved",
                variables,
                NotificationPreference.ORGANIZATION_UPDATES
            );
        } catch (Exception e) {
            log.error("Ошибка обработки OrganizationRequestApprovedEvent: requestId={}, error={}",
                event.getRequestId(), e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает отклонение запроса на организацию.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "organization-request-rejected")
    public void handleOrganizationRequestRejected(OrganizationRequestRejectedEvent event) {
        log.info("Получено событие OrganizationRequestRejectedEvent: requestId={}, userId={}",
            event.getRequestId(), event.getUserId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("organizationName", event.getOrganizationName());
            variables.put("rejectionReason", event.getRejectionReason());

            notificationService.sendTelegram(
                event.getUserId(),
                "organization.request.rejected",
                variables,
                NotificationPreference.ORGANIZATION_UPDATES
            );
        } catch (Exception e) {
            log.error("Ошибка обработки OrganizationRequestRejectedEvent: requestId={}, error={}",
                event.getRequestId(), e.getMessage(), e);
        }
    }

    // === Email Events ===

    /**
     * Обрабатывает запрос верификации email.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "email-verification-requested")
    public void handleEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        log.info("Получено событие EmailVerificationRequestedEvent: userId={}", event.getUserId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("verificationUrl", event.getVerificationUrl());

            notificationService.sendEmail(
                event.getUserId(),
                event.getEmail(),
                "auth.email-verification",
                variables
            );
        } catch (Exception e) {
            log.error("Ошибка обработки EmailVerificationRequestedEvent: userId={}, error={}",
                event.getUserId(), e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает запрос сброса пароля.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "password-reset-requested")
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Получено событие PasswordResetRequestedEvent: userId={}", event.getUserId());

        try {
            Map<String, Object> variables = new HashMap<>();
            variables.put("resetUrl", event.getResetUrl());

            notificationService.sendEmail(
                event.getUserId(),
                event.getEmail(),
                "auth.password-reset",
                variables
            );
        } catch (Exception e) {
            log.error("Ошибка обработки PasswordResetRequestedEvent: userId={}, error={}",
                event.getUserId(), e.getMessage(), e);
        }
    }

    // === Helper Methods ===

    private String formatDate(java.time.Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant);
    }

    private String buildEventUrl(String eventSlug) {
        return notificationProperties.getEventUrl(eventSlug);
    }

    private String buildOrganizationUrl(String orgSlug) {
        return notificationProperties.getOrganizationUrl(orgSlug);
    }
}
