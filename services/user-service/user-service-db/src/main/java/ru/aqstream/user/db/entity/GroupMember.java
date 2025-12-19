package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.aqstream.common.data.BaseEntity;

/**
 * Участник группы.
 *
 * <p>Связывает пользователя с группой.</p>
 */
@Entity
@Table(
    name = "group_members",
    schema = "user_service",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_group_members_group_user",
        columnNames = {"group_id", "user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Column(name = "joined_at")
    private Instant joinedAt;

    // === Фабричные методы ===

    /**
     * Создаёт запись об участнике группы.
     *
     * @param group     группа
     * @param user      пользователь
     * @param invitedBy кто пригласил (nullable если присоединился по коду)
     * @return запись об участнике группы
     */
    public static GroupMember create(Group group, User user, User invitedBy) {
        GroupMember member = new GroupMember();
        member.group = group;
        member.user = user;
        member.invitedBy = invitedBy;
        member.joinedAt = Instant.now();
        return member;
    }

    /**
     * Создаёт запись о создателе группы.
     *
     * @param group   группа
     * @param creator создатель группы
     * @return запись об участнике группы
     */
    public static GroupMember createCreator(Group group, User creator) {
        GroupMember member = new GroupMember();
        member.group = group;
        member.user = creator;
        // Создатель не приглашён
        member.invitedBy = null;
        member.joinedAt = Instant.now();
        return member;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, является ли участник создателем группы.
     *
     * @return true если участник — создатель группы
     */
    public boolean isCreator() {
        UUID creatorId = group != null ? group.getCreatedById() : null;
        UUID userId = getUserId();
        return creatorId != null && creatorId.equals(userId);
    }

    /**
     * Возвращает ID группы.
     *
     * @return UUID группы
     */
    public UUID getGroupId() {
        return group != null ? group.getId() : null;
    }

    /**
     * Возвращает ID пользователя.
     *
     * @return UUID пользователя
     */
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    /**
     * Возвращает ID пригласившего.
     *
     * @return UUID пригласившего или null
     */
    public UUID getInvitedById() {
        return invitedBy != null ? invitedBy.getId() : null;
    }
}
