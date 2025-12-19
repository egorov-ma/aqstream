package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие обновления группы.
 * Публикуется при изменении информации о группе.
 */
public class GroupUpdatedEvent extends DomainEvent {

    private final UUID groupId;
    private final UUID organizationId;
    private final String groupName;
    private final UUID updatedById;

    /**
     * Создаёт событие обновления группы.
     *
     * @param groupId        идентификатор группы
     * @param organizationId идентификатор организации
     * @param groupName      новое название группы
     * @param updatedById    идентификатор обновившего
     */
    public GroupUpdatedEvent(UUID groupId, UUID organizationId, String groupName, UUID updatedById) {
        super();
        this.groupId = groupId;
        this.organizationId = organizationId;
        this.groupName = groupName;
        this.updatedById = updatedById;
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

    public UUID getUpdatedById() {
        return updatedById;
    }

    @Override
    public String getEventType() {
        return "group.updated";
    }

    @Override
    public UUID getAggregateId() {
        return groupId;
    }
}
