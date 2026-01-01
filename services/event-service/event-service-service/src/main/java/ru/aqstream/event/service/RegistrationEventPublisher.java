package ru.aqstream.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.event.api.event.RegistrationCancelledEvent;
import ru.aqstream.event.api.event.RegistrationCreatedEvent;
import ru.aqstream.event.api.event.TicketResendRequestedEvent;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.entity.Registration;
import ru.aqstream.event.db.entity.TicketType;

/**
 * Публикация событий регистрации в RabbitMQ.
 * Отвечает за формирование и отправку domain events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationEventPublisher {

    private final EventPublisher eventPublisher;

    /**
     * Публикует событие создания регистрации.
     *
     * @param registration созданная регистрация
     */
    public void publishCreated(Registration registration) {
        Event event = registration.getEvent();
        TicketType ticketType = registration.getTicketType();

        eventPublisher.publish(new RegistrationCreatedEvent(
            registration.getId(),
            event.getId(),
            registration.getTenantId(),
            registration.getUserId(),
            event.getTitle(),
            event.getSlug(),
            event.getStartsAt(),
            ticketType.getName(),
            registration.getConfirmationCode(),
            registration.getFirstName(),
            registration.getLastName(),
            registration.getEmail()
        ));

        log.debug("Опубликовано событие RegistrationCreatedEvent: registrationId={}",
            registration.getId());
    }

    /**
     * Публикует событие отмены регистрации.
     *
     * @param registration  отменённая регистрация
     * @param byOrganizer   true если отменено организатором
     */
    public void publishCancelled(Registration registration, boolean byOrganizer) {
        Event event = registration.getEvent();
        TicketType ticketType = registration.getTicketType();

        eventPublisher.publish(new RegistrationCancelledEvent(
            registration.getId(),
            event.getId(),
            registration.getTenantId(),
            registration.getUserId(),
            event.getTitle(),
            event.getSlug(),
            event.getStartsAt(),
            ticketType.getName(),
            registration.getFirstName(),
            registration.getLastName(),
            registration.getEmail(),
            registration.getCancellationReason(),
            byOrganizer
        ));

        log.debug("Опубликовано событие RegistrationCancelledEvent: registrationId={}, byOrganizer={}",
            registration.getId(), byOrganizer);
    }

    /**
     * Публикует событие запроса повторной отправки билета.
     *
     * @param registration регистрация для повторной отправки
     */
    public void publishResendRequested(Registration registration) {
        Event event = registration.getEvent();
        TicketType ticketType = registration.getTicketType();

        eventPublisher.publish(new TicketResendRequestedEvent(
            registration.getId(),
            event.getId(),
            registration.getTenantId(),
            registration.getUserId(),
            event.getTitle(),
            event.getSlug(),
            event.getStartsAt(),
            ticketType.getName(),
            registration.getConfirmationCode(),
            registration.getFirstName(),
            registration.getLastName(),
            registration.getEmail()
        ));

        log.debug("Опубликовано событие TicketResendRequestedEvent: registrationId={}",
            registration.getId());
    }
}
