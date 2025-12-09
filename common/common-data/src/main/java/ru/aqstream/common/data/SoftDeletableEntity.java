package ru.aqstream.common.data;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;

/**
 * Базовая сущность с поддержкой soft delete.
 * Вместо физического удаления записи помечаются как удалённые через поле deletedAt.
 *
 * <p>При использовании soft delete нужно:</p>
 * <ul>
 *   <li>Добавлять {@code @Where(clause = "deleted_at IS NULL")} в entity</li>
 *   <li>Учитывать deleted_at в unique constraints</li>
 *   <li>Использовать {@link #softDelete()} вместо repository.delete()</li>
 * </ul>
 */
@MappedSuperclass
public abstract class SoftDeletableEntity extends TenantAwareEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * Время удаления сущности (UTC).
     *
     * @return момент удаления или null если не удалена
     */
    public Instant getDeletedAt() {
        return deletedAt;
    }

    /**
     * Проверяет, удалена ли сущность.
     *
     * @return true если сущность удалена
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Помечает сущность как удалённую.
     * После вызова этого метода нужно сохранить сущность через repository.
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Восстанавливает удалённую сущность.
     * После вызова этого метода нужно сохранить сущность через repository.
     */
    public void restore() {
        this.deletedAt = null;
    }
}
