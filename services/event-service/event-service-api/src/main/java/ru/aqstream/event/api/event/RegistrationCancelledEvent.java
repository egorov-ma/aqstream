package ru.aqstream.event.api.event;

import java.time.Instant;
import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие отмены регистрации.
 * Используется для отправки уведомления участнику.
 */
public class RegistrationCancelledEvent extends DomainEvent {

    private final UUID registrationId;
    private final UUID eventId;
    private final UUID tenantId;
    private final UUID userId;
    private final String eventTitle;
    private final String eventSlug;
    private final Instant eventStartsAt;
    private final String ticketTypeName;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String cancellationReason;
    private final boolean cancelledByOrganizer;

    /**
     * Создаёт событие отмены регистрации.
     *
     * @param registrationId      идентификатор регистрации
     * @param eventId             идентификатор события
     * @param tenantId            идентификатор организации
     * @param userId              идентификатор пользователя
     * @param eventTitle          название события
     * @param eventSlug           slug события
     * @param eventStartsAt       дата начала события
     * @param ticketTypeName      название типа билета
     * @param firstName           имя участника
     * @param lastName            фамилия участника
     * @param email               email участника
     * @param cancellationReason  причина отмены
     * @param cancelledByOrganizer отменено организатором
     */
    public RegistrationCancelledEvent(
            UUID registrationId,
            UUID eventId,
            UUID tenantId,
            UUID userId,
            String eventTitle,
            String eventSlug,
            Instant eventStartsAt,
            String ticketTypeName,
            String firstName,
            String lastName,
            String email,
            String cancellationReason,
            boolean cancelledByOrganizer
    ) {
        super();
        this.registrationId = registrationId;
        this.eventId = eventId;
        this.tenantId = tenantId;
        this.userId = userId;
        this.eventTitle = eventTitle;
        this.eventSlug = eventSlug;
        this.eventStartsAt = eventStartsAt;
        this.ticketTypeName = ticketTypeName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.cancellationReason = cancellationReason;
        this.cancelledByOrganizer = cancelledByOrganizer;
    }

    public UUID getRegistrationId() {
        return registrationId;
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public String getEventSlug() {
        return eventSlug;
    }

    public Instant getEventStartsAt() {
        return eventStartsAt;
    }

    public String getTicketTypeName() {
        return ticketTypeName;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public boolean isCancelledByOrganizer() {
        return cancelledByOrganizer;
    }

    @Override
    public String getEventType() {
        return "registration.cancelled";
    }

    @Override
    public UUID getAggregateId() {
        return registrationId;
    }
}
