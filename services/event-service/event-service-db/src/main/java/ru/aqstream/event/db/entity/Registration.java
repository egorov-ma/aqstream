package ru.aqstream.event.db.entity;

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
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import ru.aqstream.common.data.TenantAwareEntity;
import ru.aqstream.event.api.dto.RegistrationStatus;

/**
 * Регистрация участника на событие.
 *
 * <p>Связывает пользователя с событием через выбранный тип билета.
 * Каждая регистрация имеет уникальный confirmation_code для check-in.</p>
 *
 * <p>В Phase 2 все регистрации сразу получают статус CONFIRMED (бесплатные билеты).</p>
 */
@Entity
@Table(
    name = "registrations",
    schema = "event_service",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_registrations_confirmation_code",
        columnNames = "confirmation_code"
    )
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Registration extends TenantAwareEntity {

    // === Связи ===

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, updatable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_type_id", nullable = false, updatable = false)
    private TicketType ticketType;

    /**
     * ID пользователя (NULL для анонимных регистраций в Phase 3+).
     */
    @Column(name = "user_id")
    private UUID userId;

    // === Статус ===

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;

    /**
     * Уникальный код подтверждения для check-in (8 символов).
     */
    @Column(name = "confirmation_code", nullable = false, length = 8)
    private String confirmationCode;

    // === Данные участника ===

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    /**
     * Дополнительные поля из формы регистрации.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_fields", columnDefinition = "jsonb")
    private Map<String, Object> customFields;

    // === Резервация и отмена ===

    /**
     * Время истечения резервации (для Phase 3 с платными билетами).
     */
    @Column(name = "expires_at")
    private Instant expiresAt;

    /**
     * Время отмены регистрации.
     */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Причина отмены (опционально, при отмене организатором).
     */
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    // === Фабричные методы ===

    /**
     * Создаёт новую регистрацию на событие.
     *
     * @param event            событие
     * @param ticketType       тип билета
     * @param userId           ID пользователя
     * @param confirmationCode код подтверждения
     * @param firstName        имя участника
     * @param lastName         фамилия участника
     * @param email            email участника
     * @return новая регистрация
     */
    public static Registration create(
            Event event,
            TicketType ticketType,
            UUID userId,
            String confirmationCode,
            String firstName,
            String lastName,
            String email
    ) {
        Registration registration = new Registration();
        registration.event = event;
        registration.ticketType = ticketType;
        registration.userId = userId;
        registration.confirmationCode = confirmationCode;
        registration.firstName = firstName;
        registration.lastName = lastName;
        registration.email = email;
        registration.status = RegistrationStatus.CONFIRMED; // Phase 2: бесплатные билеты
        registration.customFields = Map.of(); // По умолчанию пустой объект
        return registration;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, подтверждена ли регистрация.
     *
     * @return true если регистрация подтверждена
     */
    public boolean isConfirmed() {
        return status == RegistrationStatus.CONFIRMED;
    }

    /**
     * Проверяет, отменена ли регистрация.
     *
     * @return true если регистрация отменена
     */
    public boolean isCancelled() {
        return status == RegistrationStatus.CANCELLED;
    }

    /**
     * Проверяет, можно ли отменить регистрацию.
     *
     * @return true если можно отменить
     */
    public boolean isCancellable() {
        return status == RegistrationStatus.CONFIRMED || status == RegistrationStatus.RESERVED;
    }

    /**
     * Отменяет регистрацию участником.
     *
     * @throws IllegalStateException если регистрация не может быть отменена
     */
    public void cancel() {
        if (!isCancellable()) {
            throw new IllegalStateException("Регистрация не может быть отменена в статусе " + status);
        }
        this.status = RegistrationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
    }

    /**
     * Отменяет регистрацию организатором с указанием причины.
     *
     * @param reason причина отмены
     * @throws IllegalStateException если регистрация не может быть отменена
     */
    public void cancelByOrganizer(String reason) {
        if (!isCancellable()) {
            throw new IllegalStateException("Регистрация не может быть отменена в статусе " + status);
        }
        this.status = RegistrationStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
    }

    /**
     * Устанавливает дополнительные поля формы.
     *
     * @param customFields поля формы
     */
    public void setCustomFields(Map<String, Object> customFields) {
        this.customFields = customFields != null ? customFields : Map.of();
    }
}
