package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие добавления участника в группу.
 * Публикуется при присоединении пользователя к группе.
 */
public class GroupMemberAddedEvent extends DomainEvent {

    private final UUID groupId;
    private final UUID userId;
    private final UUID invitedById;

    /**
     * Создаёт событие добавления участника.
     *
     * @param groupId     идентификатор группы
     * @param userId      идентификатор добавленного пользователя
     * @param invitedById идентификатор пригласившего (null если присоединился по коду сам)
     */
    public GroupMemberAddedEvent(UUID groupId, UUID userId, UUID invitedById) {
        super();
        this.groupId = groupId;
        this.userId = userId;
        this.invitedById = invitedById;
    }

    public UUID getGroupId() {
        return groupId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getInvitedById() {
        return invitedById;
    }

    @Override
    public String getEventType() {
        return "group.member.added";
    }

    @Override
    public UUID getAggregateId() {
        return groupId;
    }
}
