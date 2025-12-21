package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие создания запроса на организацию.
 * Публикуется для уведомления администраторов о новом запросе.
 */
public class OrganizationRequestCreatedEvent extends DomainEvent {

    private final UUID requestId;
    private final UUID userId;
    private final String organizationName;
    private final String slug;

    /**
     * Создаёт событие создания запроса на организацию.
     *
     * @param requestId        идентификатор запроса
     * @param userId           идентификатор пользователя
     * @param organizationName название организации
     * @param slug             URL-адрес организации
     */
    public OrganizationRequestCreatedEvent(UUID requestId, UUID userId, String organizationName, String slug) {
        super();
        this.requestId = requestId;
        this.userId = userId;
        this.organizationName = organizationName;
        this.slug = slug;
    }

    public UUID getRequestId() {
        return requestId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public String getSlug() {
        return slug;
    }

    @Override
    public String getEventType() {
        return "organization.request.created";
    }

    @Override
    public UUID getAggregateId() {
        return requestId;
    }
}
