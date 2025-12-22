package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие удаления организации.
 * Публикуется при soft delete организации.
 * Используется event-service для архивирования событий организации.
 */
public class OrganizationDeletedEvent extends DomainEvent {

    private final UUID organizationId;
    private final String organizationName;
    private final UUID deletedById;

    /**
     * Создаёт событие удаления организации.
     *
     * @param organizationId   идентификатор организации
     * @param organizationName название организации
     * @param deletedById      идентификатор удалившего (OWNER)
     */
    public OrganizationDeletedEvent(UUID organizationId, String organizationName, UUID deletedById) {
        super();
        this.organizationId = organizationId;
        this.organizationName = organizationName;
        this.deletedById = deletedById;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public UUID getDeletedById() {
        return deletedById;
    }

    @Override
    public String getEventType() {
        return "organization.deleted";
    }

    @Override
    public UUID getAggregateId() {
        return organizationId;
    }
}
