package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие одобрения запроса на организацию.
 * Публикуется для уведомления пользователя об одобрении.
 */
public class OrganizationRequestApprovedEvent extends DomainEvent {

    private final UUID requestId;
    private final UUID userId;
    private final String organizationName;
    private final String slug;
    private final UUID approvedById;

    /**
     * Создаёт событие одобрения запроса.
     *
     * @param requestId        идентификатор запроса
     * @param userId           идентификатор пользователя
     * @param organizationName название организации
     * @param slug             URL-адрес организации
     * @param approvedById     идентификатор администратора
     */
    public OrganizationRequestApprovedEvent(
        UUID requestId,
        UUID userId,
        String organizationName,
        String slug,
        UUID approvedById
    ) {
        super();
        this.requestId = requestId;
        this.userId = userId;
        this.organizationName = organizationName;
        this.slug = slug;
        this.approvedById = approvedById;
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

    public UUID getApprovedById() {
        return approvedById;
    }

    @Override
    public String getEventType() {
        return "organization.request.approved";
    }

    @Override
    public UUID getAggregateId() {
        return requestId;
    }
}
