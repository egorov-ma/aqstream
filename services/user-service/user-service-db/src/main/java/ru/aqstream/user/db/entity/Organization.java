package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import ru.aqstream.common.data.BaseEntity;

/**
 * Организация — tenant в системе AqStream.
 *
 * <p>Глобальная таблица (без tenant_id).
 * Organization.id используется как tenant_id для других сервисов.</p>
 *
 * <p>Поддерживает soft delete через поле deletedAt.</p>
 */
@Entity
@Table(name = "organizations", schema = "user_service")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Organization extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 50)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb")
    private String settings = "{}";

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // === Фабричные методы ===

    /**
     * Создаёт новую организацию.
     *
     * @param owner       владелец организации
     * @param name        название организации
     * @param slug        URL-slug
     * @param description описание (nullable)
     * @return новая организация
     */
    public static Organization create(User owner, String name, String slug, String description) {
        Organization org = new Organization();
        org.owner = owner;
        org.name = name;
        org.slug = normalizeSlug(slug);
        org.description = description;
        org.settings = "{}";
        return org;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, удалена ли организация.
     *
     * @return true если организация удалена
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Помечает организацию как удалённую (soft delete).
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Восстанавливает удалённую организацию.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Обновляет основную информацию об организации.
     *
     * @param name        новое название
     * @param description новое описание
     */
    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * Обновляет логотип организации.
     *
     * @param logoUrl URL нового логотипа
     */
    public void updateLogo(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    /**
     * Возвращает ID владельца.
     *
     * @return UUID владельца
     */
    public UUID getOwnerId() {
        return owner != null ? owner.getId() : null;
    }

    /**
     * Устанавливает slug с нормализацией.
     *
     * @param slug URL-slug
     */
    public void setSlug(String slug) {
        this.slug = normalizeSlug(slug);
    }

    /**
     * Нормализует slug (приводит к нижнему регистру, удаляет пробелы).
     *
     * @param slug исходный slug
     * @return нормализованный slug
     */
    private static String normalizeSlug(String slug) {
        return slug != null ? slug.toLowerCase().trim() : null;
    }
}
