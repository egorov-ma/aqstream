package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import ru.aqstream.common.data.BaseEntity;
import ru.aqstream.user.api.dto.OrganizationRequestStatus;

/**
 * Запрос на создание организации.
 *
 * <p>Глобальная таблица (без tenant_id).
 * Требует одобрения администратора платформы.</p>
 */
@Entity
@Table(name = "organization_requests", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationRequest extends BaseEntity {

    /**
     * Время резервирования slug после одобрения (в днях).
     */
    public static final int SLUG_RESERVATION_DAYS = 7;

    /**
     * Минимальная длина slug.
     */
    public static final int SLUG_MIN_LENGTH = 3;

    /**
     * Максимальная длина slug.
     */
    public static final int SLUG_MAX_LENGTH = 50;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "slug", nullable = false, length = 50)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OrganizationRequestStatus status = OrganizationRequestStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private User reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    // === Фабричные методы ===

    /**
     * Создаёт новый запрос на создание организации.
     *
     * @param user        пользователь-заявитель
     * @param name        название организации
     * @param slug        URL-slug
     * @param description описание (nullable)
     * @return новый запрос
     */
    public static OrganizationRequest create(User user, String name, String slug, String description) {
        OrganizationRequest request = new OrganizationRequest();
        request.user = user;
        request.name = name;
        request.slug = normalizeSlug(slug);
        request.description = description;
        request.status = OrganizationRequestStatus.PENDING;
        return request;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, ожидает ли запрос рассмотрения.
     *
     * @return true если статус PENDING
     */
    public boolean isPending() {
        return status == OrganizationRequestStatus.PENDING;
    }

    /**
     * Проверяет, одобрен ли запрос.
     *
     * @return true если статус APPROVED
     */
    public boolean isApproved() {
        return status == OrganizationRequestStatus.APPROVED;
    }

    /**
     * Проверяет, отклонён ли запрос.
     *
     * @return true если статус REJECTED
     */
    public boolean isRejected() {
        return status == OrganizationRequestStatus.REJECTED;
    }

    /**
     * Проверяет, истёк ли срок резервации slug.
     * Slug резервируется на 7 дней после одобрения.
     *
     * @return true если запрос одобрен и прошло более 7 дней
     */
    public boolean isSlugReservationExpired() {
        if (!isApproved() || reviewedAt == null) {
            return false;
        }
        return Instant.now().isAfter(reviewedAt.plus(SLUG_RESERVATION_DAYS, java.time.temporal.ChronoUnit.DAYS));
    }

    /**
     * Проверяет, активна ли резервация slug.
     * Резервация активна если запрос одобрен и не прошло 7 дней.
     *
     * @return true если резервация активна
     */
    public boolean isSlugReservationActive() {
        return isApproved() && !isSlugReservationExpired();
    }

    /**
     * Одобряет запрос.
     * Проверка статуса должна быть выполнена в сервисе перед вызовом этого метода.
     *
     * @param admin администратор, одобривший запрос
     */
    public void approve(User admin) {
        this.status = OrganizationRequestStatus.APPROVED;
        this.reviewedBy = admin;
        this.reviewedAt = Instant.now();
    }

    /**
     * Отклоняет запрос.
     * Проверка статуса должна быть выполнена в сервисе перед вызовом этого метода.
     *
     * @param admin   администратор, отклонивший запрос
     * @param comment причина отклонения
     */
    public void reject(User admin, String comment) {
        this.status = OrganizationRequestStatus.REJECTED;
        this.reviewedBy = admin;
        this.reviewComment = comment;
        this.reviewedAt = Instant.now();
    }

    /**
     * Возвращает ID пользователя-заявителя.
     *
     * @return UUID пользователя
     */
    public UUID getUserId() {
        return user != null ? user.getId() : null;
    }

    /**
     * Возвращает ID администратора, рассмотревшего запрос.
     *
     * @return UUID администратора или null
     */
    public UUID getReviewedById() {
        return reviewedBy != null ? reviewedBy.getId() : null;
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
