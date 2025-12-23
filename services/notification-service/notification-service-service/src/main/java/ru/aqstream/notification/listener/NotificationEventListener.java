package ru.aqstream.notification.listener;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.event.EventCancelledEvent;
import ru.aqstream.event.api.event.EventUpdatedEvent;
import ru.aqstream.event.api.event.RegistrationCancelledEvent;
import ru.aqstream.event.api.event.RegistrationCreatedEvent;
import ru.aqstream.event.client.EventClient;
import ru.aqstream.notification.config.NotificationProperties;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.service.NotificationService;
import ru.aqstream.user.api.event.EmailVerificationRequestedEvent;
import ru.aqstream.user.api.event.OrganizationRequestApprovedEvent;
import ru.aqstream.user.api.event.OrganizationRequestRejectedEvent;
import ru.aqstream.user.api.event.PasswordResetRequestedEvent;
import ru.aqstream.user.api.event.UserRegisteredEvent;

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
    private final EventClient eventClient;

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
     * Отправляет уведомление всем активным участникам.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "event-cancelled")
    public void handleEventCancelled(EventCancelledEvent event) {
        log.info("Получено событие EventCancelledEvent: eventId={}, tenantId={}",
            event.getEventId(), event.getTenantId());

        try {
            // Получаем список всех активных регистраций через EventClient
            List<RegistrationDto> registrations = eventClient.findActiveRegistrations(
                event.getEventId(),
                event.getTenantId()
            );

            log.info("Массовая рассылка при отмене события: eventId={}, участников={}",
                event.getEventId(), registrations.size());

            // Отправляем уведомление каждому участнику
            int sentCount = 0;
            int failedCount = 0;

            for (RegistrationDto registration : registrations) {
                try {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("firstName", registration.firstName());
                    variables.put("eventTitle", event.getTitle());
                    variables.put("eventDate", formatDate(event.getStartsAt()));

                    notificationService.sendTelegram(
                        registration.userId(),
                        "event.cancelled",
                        variables,
                        NotificationPreference.EVENT_CHANGES
                    );
                    sentCount++;

                    // Rate limiting: небольшая задержка между отправками
                    if (sentCount % 30 == 0) {
                        Thread.sleep(1000); // 1 секунда каждые 30 сообщений
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Прерывание массовой рассылки: eventId={}", event.getEventId());
                    break;
                } catch (Exception e) {
                    failedCount++;
                    log.debug("Ошибка отправки уведомления: userId={}, error={}",
                        registration.userId(), e.getMessage());
                }
            }

            log.info("Массовая рассылка завершена: eventId={}, отправлено={}, ошибок={}",
                event.getEventId(), sentCount, failedCount);

        } catch (Exception e) {
            log.error("Ошибка обработки EventCancelledEvent: eventId={}, error={}",
                event.getEventId(), e.getMessage(), e);
        }
    }

    /**
     * Обрабатывает изменения в событии.
     * Отправляет уведомление всем активным участникам о произошедших изменениях.
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "event-updated")
    public void handleEventUpdated(EventUpdatedEvent event) {
        log.info("Получено событие EventUpdatedEvent: eventId={}, tenantId={}",
            event.getEventId(), event.getTenantId());

        try {
            // Получаем список всех активных регистраций через EventClient
            List<RegistrationDto> registrations = eventClient.findActiveRegistrations(
                event.getEventId(),
                event.getTenantId()
            );

            log.info("Массовая рассылка при изменении события: eventId={}, участников={}",
                event.getEventId(), registrations.size());

            // Отправляем уведомление каждому участнику
            int sentCount = 0;
            int failedCount = 0;

            for (RegistrationDto registration : registrations) {
                try {
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("firstName", registration.firstName());
                    variables.put("eventTitle", event.getTitle());
                    variables.put("eventDate", formatDate(event.getStartsAt()));

                    notificationService.sendTelegram(
                        registration.userId(),
                        "event.changed",
                        variables,
                        NotificationPreference.EVENT_CHANGES
                    );
                    sentCount++;

                    // Rate limiting: небольшая задержка между отправками
                    if (sentCount % 30 == 0) {
                        Thread.sleep(1000); // 1 секунда каждые 30 сообщений
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Прерывание массовой рассылки: eventId={}", event.getEventId());
                    break;
                } catch (Exception e) {
                    failedCount++;
                    log.debug("Ошибка отправки уведомления: userId={}, error={}",
                        registration.userId(), e.getMessage());
                }
            }

            log.info("Массовая рассылка завершена: eventId={}, отправлено={}, ошибок={}",
                event.getEventId(), sentCount, failedCount);

        } catch (Exception e) {
            log.error("Ошибка обработки EventUpdatedEvent: eventId={}, error={}",
                event.getEventId(), e.getMessage(), e);
        }
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

    // === User Events ===

    /**
     * Обрабатывает регистрацию нового пользователя.
     * Отправляет приветственное уведомление через Telegram (если есть chat_id).
     */
    @RabbitListener(queues = NOTIFICATION_QUEUE, id = "user-registered")
    public void handleUserRegistered(UserRegisteredEvent event) {
        log.info("Получено событие UserRegisteredEvent: userId={}, source={}",
            event.getUserId(), event.getSource());

        try {
            // Отправляем приветственное уведомление только если есть Telegram chat_id
            if (event.getTelegramChatId() != null) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("firstName", event.getFirstName());

                notificationService.sendTelegram(
                    event.getUserId(),
                    "user.welcome",
                    variables
                );
            } else {
                log.debug("Пропуск приветственного уведомления - нет Telegram chat_id: userId={}",
                    event.getUserId());
            }
        } catch (Exception e) {
            log.error("Ошибка обработки UserRegisteredEvent: userId={}, error={}",
                event.getUserId(), e.getMessage(), e);
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
