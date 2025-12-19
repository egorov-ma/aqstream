package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.aqstream.common.data.BaseEntity;
import ru.aqstream.user.api.util.InviteCodeGenerator;

/**
 * Группа для приватных событий внутри организации.
 *
 * <p>Позволяет организовывать события, доступные только участникам группы.</p>
 */
@Entity
@Table(name = "groups", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Group extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "invite_code", nullable = false, unique = true, length = 8)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // === Фабричные методы ===

    /**
     * Создаёт новую группу.
     *
     * @param organization организация
     * @param createdBy    создатель группы
     * @param name         название группы
     * @param description  описание группы (nullable)
     * @return новая группа
     */
    public static Group create(Organization organization, User createdBy, String name, String description) {
        Group group = new Group();
        group.organization = organization;
        group.createdBy = createdBy;
        group.name = name;
        group.description = description;
        group.inviteCode = InviteCodeGenerator.generate();
        return group;
    }

    // === Бизнес-методы ===

    /**
     * Генерирует новый инвайт-код для группы.
     */
    public void regenerateInviteCode() {
        this.inviteCode = InviteCodeGenerator.generate();
    }

    /**
     * Обновляет основную информацию о группе.
     *
     * @param name        новое название
     * @param description новое описание
     */
    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
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
     * Возвращает ID создателя.
     *
     * @return UUID создателя
     */
    public UUID getCreatedById() {
        return createdBy != null ? createdBy.getId() : null;
    }
}
