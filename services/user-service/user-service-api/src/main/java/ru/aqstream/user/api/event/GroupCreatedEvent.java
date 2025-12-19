package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие создания группы.
 * Публикуется при успешном создании группы в организации.
 */
public class GroupCreatedEvent extends DomainEvent {

    private final UUID groupId;
    private final UUID organizationId;
    private final String groupName;
    private final UUID createdById;

    /**
     * Создаёт событие создания группы.
     *
     * @param groupId        идентификатор созданной группы
     * @param organizationId идентификатор организации
     * @param groupName      название группы
     * @param createdById    идентификатор создателя
     */
    public GroupCreatedEvent(UUID groupId, UUID organizationId, String groupName, UUID createdById) {
        super();
        this.groupId = groupId;
        this.organizationId = organizationId;
        this.groupName = groupName;
        this.createdById = createdById;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getGroupName() {
        return groupName;
    }

    public UUID getCreatedById() {
        return createdById;
    }

    @Override
    public String getEventType() {
        return "group.created";
    }

    @Override
    public UUID getAggregateId() {
        return groupId;
    }
}
