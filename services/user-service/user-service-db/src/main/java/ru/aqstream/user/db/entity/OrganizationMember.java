package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import ru.aqstream.user.api.dto.OrganizationRole;

/**
 * Член организации.
 *
 * <p>Связывает пользователя с организацией и определяет его роль.</p>
 */
@Entity
@Table(
    name = "organization_members",
    schema = "user_service",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_organization_members_org_user",
        columnNames = {"organization_id", "user_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OrganizationRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by")
    private User invitedBy;

    @Column(name = "joined_at")
    private Instant joinedAt;

    // === Фабричные методы ===

    /**
     * Создаёт запись о владельце организации.
     *
     * @param organization организация
     * @param owner        пользователь-владелец
     * @return запись о члене организации
     */
    public static OrganizationMember createOwner(Organization organization, User owner) {
        OrganizationMember member = new OrganizationMember();
        member.organization = organization;
        member.user = owner;
        member.role = OrganizationRole.OWNER;
        member.joinedAt = Instant.now();
        // Владелец не приглашён, он создатель
        member.invitedBy = null;
        return member;
    }

    /**
     * Создаёт запись о модераторе организации.
     *
     * @param organization организация
     * @param user         пользователь
     * @param invitedBy    кто пригласил
     * @return запись о члене организации
     */
    public static OrganizationMember createModerator(Organization organization, User user, User invitedBy) {
        OrganizationMember member = new OrganizationMember();
        member.organization = organization;
        member.user = user;
        member.role = OrganizationRole.MODERATOR;
        member.invitedBy = invitedBy;
        member.joinedAt = Instant.now();
        return member;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, является ли член владельцем.
     *
     * @return true если роль OWNER
     */
    public boolean isOwner() {
        return role == OrganizationRole.OWNER;
    }

    /**
     * Проверяет, является ли член модератором.
     *
     * @return true если роль MODERATOR
     */
    public boolean isModerator() {
        return role == OrganizationRole.MODERATOR;
    }

    /**
     * Возвращает ID организации.
     *
     * @return UUID организации
     */
    public UUID getOrganizationId() {
        return organization != null ? organization.getId() : null;
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
