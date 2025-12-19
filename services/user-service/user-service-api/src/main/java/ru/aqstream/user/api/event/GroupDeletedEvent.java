package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие удаления группы.
 * Публикуется при удалении группы из организации.
 */
public class GroupDeletedEvent extends DomainEvent {

    private final UUID groupId;
    private final UUID organizationId;
    private final UUID deletedById;

    /**
     * Создаёт событие удаления группы.
     *
     * @param groupId        идентификатор удалённой группы
     * @param organizationId идентификатор организации
     * @param deletedById    идентификатор удалившего
     */
    public GroupDeletedEvent(UUID groupId, UUID organizationId, UUID deletedById) {
        super();
        this.groupId = groupId;
        this.organizationId = organizationId;
        this.deletedById = deletedById;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getDeletedById() {
        return deletedById;
    }

    @Override
    public String getEventType() {
        return "group.deleted";
    }

    @Override
    public UUID getAggregateId() {
        return groupId;
    }
}
