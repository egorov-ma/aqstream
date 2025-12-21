package ru.aqstream.event.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.aqstream.common.data.BaseEntity;

/**
 * Тип билета для события.
 *
 * <p>Название типа билета — произвольный текст от организатора (не enum).
 * Например: «VIP-место», «Стандарт», «Студенческий», «Онлайн-участие».</p>
 *
 * <p>В Phase 2 все билеты бесплатные (priceCents = 0).</p>
 *
 * <p>Доступность вычисляется как: available = quantity - soldCount - reservedCount</p>
 *
 * <p>Для предотвращения overselling используется optimistic locking (@Version).</p>
 */
@Entity
@Table(name = "ticket_types", schema = "event_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketType extends BaseEntity {

    // === Поля ===

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private Event event;

    /**
     * Название типа билета (произвольный текст от организатора).
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Описание типа билета (опционально).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Цена в копейках (0 = бесплатный билет).
     * В Phase 2 всегда 0.
     */
    @Column(name = "price_cents", nullable = false)
    private int priceCents = 0;

    /**
     * Валюта (ISO 4217).
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "RUB";

    /**
     * Общее количество билетов (null = unlimited).
     */
    @Column(name = "quantity")
    private Integer quantity;

    /**
     * Количество проданных билетов.
     */
    @Column(name = "sold_count", nullable = false)
    private int soldCount = 0;

    /**
     * Количество зарезервированных билетов (ожидающих оплаты).
     */
    @Column(name = "reserved_count", nullable = false)
    private int reservedCount = 0;

    /**
     * Время резервации в минутах (null = без резервации, Phase 3).
     */
    @Column(name = "reservation_minutes")
    private Integer reservationMinutes;

    /**
     * Процент предоплаты (null = полная оплата, Phase 3).
     */
    @Column(name = "prepayment_percent")
    private Integer prepaymentPercent;

    /**
     * Начало периода продаж (null = без ограничения).
     */
    @Column(name = "sales_start")
    private Instant salesStart;

    /**
     * Окончание периода продаж (null = без ограничения).
     */
    @Column(name = "sales_end")
    private Instant salesEnd;

    /**
     * Порядок сортировки при отображении.
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /**
     * Активен ли тип билета (для деактивации вместо удаления).
     */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /**
     * Версия для optimistic locking (предотвращение overselling).
     */
    @Version
    @Column(name = "version", nullable = false)
    private int version = 0;

    // === Фабричные методы ===

    /**
     * Создаёт новый тип билета для события.
     *
     * @param event событие
     * @param name  название типа билета
     * @return новый тип билета
     */
    public static TicketType create(Event event, String name) {
        TicketType ticketType = new TicketType();
        ticketType.event = event;
        ticketType.name = name;
        ticketType.priceCents = 0; // Phase 2: бесплатные билеты
        ticketType.currency = "RUB";
        ticketType.active = true;
        return ticketType;
    }

    // === Бизнес-методы ===

    /**
     * Вычисляет количество доступных билетов.
     *
     * @return количество доступных билетов или null если unlimited
     */
    public Integer getAvailable() {
        if (quantity == null) {
            return null; // unlimited
        }
        return Math.max(0, quantity - soldCount - reservedCount);
    }

    /**
     * Проверяет, распроданы ли билеты.
     *
     * @return true если билеты распроданы
     */
    public boolean isSoldOut() {
        if (quantity == null) {
            return false; // unlimited
        }
        return getAvailable() <= 0;
    }

    /**
     * Проверяет, открыты ли продажи.
     *
     * @return true если продажи открыты
     */
    public boolean isSalesOpen() {
        if (!active) {
            return false;
        }
        Instant now = Instant.now();
        boolean started = salesStart == null || !now.isBefore(salesStart);
        boolean notEnded = salesEnd == null || now.isBefore(salesEnd);
        return started && notEnded;
    }

    /**
     * Проверяет, можно ли зарегистрироваться на данный тип билета.
     *
     * @return true если регистрация возможна
     */
    public boolean isAvailableForRegistration() {
        return active && isSalesOpen() && !isSoldOut();
    }

    /**
     * Увеличивает счётчик проданных билетов.
     *
     * @throws IllegalStateException если билеты распроданы
     */
    public void incrementSoldCount() {
        if (isSoldOut()) {
            throw new IllegalStateException("Билеты данного типа распроданы");
        }
        this.soldCount++;
    }

    /**
     * Уменьшает счётчик проданных билетов (при отмене).
     */
    public void decrementSoldCount() {
        if (this.soldCount > 0) {
            this.soldCount--;
        }
    }

    /**
     * Увеличивает счётчик зарезервированных билетов.
     *
     * @throws IllegalStateException если билеты распроданы
     */
    public void incrementReservedCount() {
        if (isSoldOut()) {
            throw new IllegalStateException("Билеты данного типа распроданы");
        }
        this.reservedCount++;
    }

    /**
     * Уменьшает счётчик зарезервированных билетов.
     */
    public void decrementReservedCount() {
        if (this.reservedCount > 0) {
            this.reservedCount--;
        }
    }

    /**
     * Активирует тип билета.
     */
    public void activate() {
        this.active = true;
    }

    /**
     * Деактивирует тип билета (вместо удаления).
     */
    public void deactivate() {
        this.active = false;
    }

    // === Обновление полей ===

    /**
     * Обновляет основную информацию о типе билета.
     *
     * @param name        новое название
     * @param description новое описание
     */
    public void updateInfo(String name, String description) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        this.description = description;
    }

    /**
     * Обновляет количество билетов.
     * Нельзя установить значение меньше уже проданных.
     *
     * @param quantity новое количество (null = unlimited)
     * @throws IllegalArgumentException если новое количество меньше проданных
     */
    public void updateQuantity(Integer quantity) {
        if (quantity != null && quantity < soldCount + reservedCount) {
            throw new IllegalArgumentException(
                "Количество билетов не может быть меньше уже проданных/зарезервированных"
            );
        }
        this.quantity = quantity;
    }

    /**
     * Обновляет период продаж.
     *
     * @param salesStart начало продаж
     * @param salesEnd   окончание продаж
     */
    public void updateSalesPeriod(Instant salesStart, Instant salesEnd) {
        this.salesStart = salesStart;
        this.salesEnd = salesEnd;
    }
}
