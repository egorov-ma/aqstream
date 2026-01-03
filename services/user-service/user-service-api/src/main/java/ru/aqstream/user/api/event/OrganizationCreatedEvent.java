package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие создания организации.
 * Публикуется для уведомления других сервисов о новой организации (tenantId).
 */
public class OrganizationCreatedEvent extends DomainEvent {

    private final UUID organizationId;
    private final String name;
    private final String slug;
    private final UUID ownerId;

    /**
     * Создаёт событие создания организации.
     *
     * @param organizationId идентификатор организации
     * @param name           название организации
     * @param slug           URL-slug организации
     * @param ownerId        идентификатор владельца
     */
    public OrganizationCreatedEvent(
        UUID organizationId,
        String name,
        String slug,
        UUID ownerId
    ) {
        super();
        this.organizationId = organizationId;
        this.name = name;
        this.slug = slug;
        this.ownerId = ownerId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getSlug() {
        return slug;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    @Override
    public String getEventType() {
        return "organization.created";
    }

    @Override
    public UUID getAggregateId() {
        return organizationId;
    }
}
