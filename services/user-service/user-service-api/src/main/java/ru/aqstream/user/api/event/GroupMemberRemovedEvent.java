package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие удаления участника из группы.
 * Публикуется при выходе или удалении пользователя из группы.
 */
public class GroupMemberRemovedEvent extends DomainEvent {

    private final UUID groupId;
    private final UUID userId;
    private final UUID removedById;
    private final boolean selfRemoval;

    /**
     * Создаёт событие удаления участника.
     *
     * @param groupId     идентификатор группы
     * @param userId      идентификатор удалённого пользователя
     * @param removedById идентификатор удалившего
     * @param selfRemoval true если пользователь вышел сам, false если был удалён
     */
    public GroupMemberRemovedEvent(UUID groupId, UUID userId, UUID removedById, boolean selfRemoval) {
        super();
        this.groupId = groupId;
        this.userId = userId;
        this.removedById = removedById;
        this.selfRemoval = selfRemoval;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getRemovedById() {
        return removedById;
    }

    public boolean isSelfRemoval() {
        return selfRemoval;
    }

    @Override
    public String getEventType() {
        return "group.member.removed";
    }

    @Override
    public UUID getAggregateId() {
        return groupId;
    }
}
