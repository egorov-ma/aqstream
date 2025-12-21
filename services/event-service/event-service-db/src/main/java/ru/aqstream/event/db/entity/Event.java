package ru.aqstream.event.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;
import ru.aqstream.common.data.SoftDeletableEntity;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.LocationType;
import ru.aqstream.event.api.dto.ParticipantsVisibility;
import ru.aqstream.event.api.exception.EventInPastException;
import ru.aqstream.event.api.exception.InvalidEventStatusTransitionException;

/**
 * Событие — центральная бизнес-сущность платформы.
 *
 * <p>Жизненный цикл:</p>
 * <pre>
 * DRAFT → PUBLISHED → COMPLETED
 *   ↓         ↓
 * CANCELLED  CANCELLED
 * </pre>
 */
@Entity
@Table(name = "events", schema = "event_service")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Event extends SoftDeletableEntity {

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "slug", nullable = false, length = 255)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = "Europe/Moscow";

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 50)
    private LocationType locationType = LocationType.ONLINE;

    @Column(name = "location_address", columnDefinition = "TEXT")
    private String locationAddress;

    @Column(name = "online_url", columnDefinition = "TEXT")
    private String onlineUrl;

    @Column(name = "max_capacity")
    private Integer maxCapacity;

    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "participants_visibility", length = 20)
    private ParticipantsVisibility participantsVisibility = ParticipantsVisibility.CLOSED;

    @Column(name = "group_id")
    private UUID groupId;

    // === Фабричные методы ===

    /**
     * Создаёт новое событие в статусе DRAFT.
     *
     * @param title    название события
     * @param slug     URL-slug
     * @param startsAt дата начала
     * @param timezone часовой пояс
     * @return новое событие
     */
    public static Event create(String title, String slug, Instant startsAt, String timezone) {
        Event event = new Event();
        event.title = title;
        event.slug = normalizeSlug(slug);
        event.startsAt = startsAt;
        event.timezone = timezone != null ? timezone : "Europe/Moscow";
        event.status = EventStatus.DRAFT;
        event.locationType = LocationType.ONLINE;
        event.participantsVisibility = ParticipantsVisibility.CLOSED;
        return event;
    }

    // === Бизнес-методы жизненного цикла ===

    /**
     * Публикует событие (DRAFT → PUBLISHED).
     *
     * @throws InvalidEventStatusTransitionException если событие не в статусе DRAFT
     * @throws EventInPastException                  если дата начала в прошлом
     */
    public void publish() {
        if (status != EventStatus.DRAFT) {
            throw new InvalidEventStatusTransitionException(status, EventStatus.PUBLISHED);
        }
        if (startsAt.isBefore(Instant.now())) {
            throw new EventInPastException();
        }
        this.status = EventStatus.PUBLISHED;
    }

    /**
     * Снимает событие с публикации (PUBLISHED → DRAFT).
     *
     * @throws InvalidEventStatusTransitionException если событие не в статусе PUBLISHED
     */
    public void unpublish() {
        if (status != EventStatus.PUBLISHED) {
            throw new InvalidEventStatusTransitionException(status, EventStatus.DRAFT);
        }
        this.status = EventStatus.DRAFT;
    }

    /**
     * Отменяет событие (любой статус кроме COMPLETED → CANCELLED).
     *
     * @throws InvalidEventStatusTransitionException если событие уже завершено
     */
    public void cancel() {
        if (status == EventStatus.COMPLETED) {
            throw new InvalidEventStatusTransitionException(status, EventStatus.CANCELLED);
        }
        if (status == EventStatus.CANCELLED) {
            throw new InvalidEventStatusTransitionException("Событие уже отменено");
        }
        this.status = EventStatus.CANCELLED;
    }

    /**
     * Завершает событие (PUBLISHED → COMPLETED).
     *
     * @throws InvalidEventStatusTransitionException если событие не в статусе PUBLISHED
     */
    public void complete() {
        if (status != EventStatus.PUBLISHED) {
            throw new InvalidEventStatusTransitionException(status, EventStatus.COMPLETED);
        }
        this.status = EventStatus.COMPLETED;
    }

    // === Проверки состояния ===

    /**
     * Проверяет, является ли событие черновиком.
     *
     * @return true если статус DRAFT
     */
    public boolean isDraft() {
        return status == EventStatus.DRAFT;
    }

    /**
     * Проверяет, опубликовано ли событие.
     *
     * @return true если статус PUBLISHED
     */
    public boolean isPublished() {
        return status == EventStatus.PUBLISHED;
    }

    /**
     * Проверяет, отменено ли событие.
     *
     * @return true если статус CANCELLED
     */
    public boolean isCancelled() {
        return status == EventStatus.CANCELLED;
    }

    /**
     * Проверяет, завершено ли событие.
     *
     * @return true если статус COMPLETED
     */
    public boolean isCompleted() {
        return status == EventStatus.COMPLETED;
    }

    /**
     * Проверяет, можно ли редактировать событие.
     * В DRAFT можно редактировать всё, в PUBLISHED — с ограничениями.
     *
     * @return true если можно редактировать
     */
    public boolean isEditable() {
        return status == EventStatus.DRAFT || status == EventStatus.PUBLISHED;
    }

    /**
     * Проверяет, открыта ли регистрация.
     *
     * @return true если регистрация открыта
     */
    public boolean isRegistrationOpen() {
        if (status != EventStatus.PUBLISHED) {
            return false;
        }
        Instant now = Instant.now();
        boolean opened = registrationOpensAt == null || !now.isBefore(registrationOpensAt);
        boolean notClosed = registrationClosesAt == null || now.isBefore(registrationClosesAt);
        return opened && notClosed;
    }

    // === Обновление полей ===

    /**
     * Обновляет основную информацию о событии.
     *
     * @param title       новое название
     * @param description новое описание
     */
    public void updateInfo(String title, String description) {
        if (title != null && !title.isBlank()) {
            this.title = title;
        }
        this.description = description;
    }

    /**
     * Обновляет даты события.
     *
     * @param startsAt новая дата начала
     * @param endsAt   новая дата окончания
     */
    public void updateDates(Instant startsAt, Instant endsAt) {
        if (startsAt != null) {
            this.startsAt = startsAt;
        }
        this.endsAt = endsAt;
    }

    /**
     * Обновляет локацию события.
     *
     * @param locationType    тип локации
     * @param locationAddress физический адрес
     * @param onlineUrl       онлайн ссылка
     */
    public void updateLocation(LocationType locationType, String locationAddress, String onlineUrl) {
        if (locationType != null) {
            this.locationType = locationType;
        }
        this.locationAddress = locationAddress;
        this.onlineUrl = onlineUrl;
    }

    /**
     * Обновляет настройки регистрации.
     *
     * @param maxCapacity          максимальная вместимость
     * @param registrationOpensAt  дата открытия регистрации
     * @param registrationClosesAt дата закрытия регистрации
     */
    public void updateRegistration(Integer maxCapacity, Instant registrationOpensAt, Instant registrationClosesAt) {
        this.maxCapacity = maxCapacity;
        this.registrationOpensAt = registrationOpensAt;
        this.registrationClosesAt = registrationClosesAt;
    }

    /**
     * Обновляет настройки видимости.
     *
     * @param isPublic               публичность
     * @param participantsVisibility видимость участников
     */
    public void updateVisibility(boolean isPublic, ParticipantsVisibility participantsVisibility) {
        this.isPublic = isPublic;
        if (participantsVisibility != null) {
            this.participantsVisibility = participantsVisibility;
        }
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
