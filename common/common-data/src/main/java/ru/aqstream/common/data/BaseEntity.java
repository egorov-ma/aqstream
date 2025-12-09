package ru.aqstream.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Базовая сущность для всех JPA entities.
 * Содержит общие поля: id, createdAt, updatedAt.
 *
 * <p>Для работы аудита требуется включить {@code @EnableJpaAuditing} в конфигурации.</p>
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Уникальный идентификатор сущности.
     *
     * @return UUID сущности
     */
    public UUID getId() {
        return id;
    }

    /**
     * Устанавливает идентификатор сущности.
     * Используется только для тестов или миграций.
     *
     * @param id идентификатор
     */
    protected void setId(UUID id) {
        this.id = id;
    }

    /**
     * Время создания сущности (UTC).
     *
     * @return момент создания
     */
    public Instant getCreatedAt() {
        return createdAt;
    }

    /**
     * Время последнего обновления сущности (UTC).
     *
     * @return момент обновления
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Проверяет, является ли сущность новой (ещё не сохранённой).
     *
     * @return true если сущность новая
     */
    public boolean isNew() {
        return id == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        // Сравниваем только по id если он установлен
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        // Используем константу для новых объектов
        return id != null ? id.hashCode() : 31;
    }
}
