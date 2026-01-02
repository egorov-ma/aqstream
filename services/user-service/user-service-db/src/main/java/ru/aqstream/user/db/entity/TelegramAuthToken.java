package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Токен для авторизации через Telegram бота.
 *
 * <p>Жизненный цикл:
 * <ol>
 *   <li>Пользователь нажимает "Войти через Telegram" — создаётся токен со статусом PENDING</li>
 *   <li>Пользователь переходит в бота и подтверждает вход — статус меняется на CONFIRMED</li>
 *   <li>Frontend получает уведомление через WebSocket и запрашивает JWT — статус меняется на USED</li>
 *   <li>Если токен не подтверждён в течение 5 минут — статус меняется на EXPIRED</li>
 * </ol>
 * </p>
 *
 * <p>В отличие от TelegramLinkToken, этот токен не привязан к пользователю изначально —
 * пользователь определяется только после подтверждения в боте.</p>
 */
@Entity
@Table(name = "telegram_auth_tokens", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"token"})
public class TelegramAuthToken {

    /**
     * Время жизни токена — 5 минут.
     */
    public static final long EXPIRATION_MINUTES = 5;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    /**
     * Уникальный токен для идентификации сессии авторизации.
     */
    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    /**
     * Telegram ID пользователя. Заполняется при подтверждении в боте.
     */
    @Column(name = "telegram_id", length = 64)
    private String telegramId;

    /**
     * Имя пользователя в Telegram. Заполняется при подтверждении.
     */
    @Column(name = "telegram_first_name")
    private String telegramFirstName;

    /**
     * Фамилия пользователя в Telegram. Заполняется при подтверждении.
     */
    @Column(name = "telegram_last_name")
    private String telegramLastName;

    /**
     * Username пользователя в Telegram. Заполняется при подтверждении.
     */
    @Column(name = "telegram_username")
    private String telegramUsername;

    /**
     * Chat ID для отправки сообщений. Заполняется при подтверждении.
     */
    @Column(name = "telegram_chat_id", length = 64)
    private String telegramChatId;

    /**
     * Текущий статус токена.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AuthTokenStatus status = AuthTokenStatus.PENDING;

    /**
     * Время истечения токена.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * Время подтверждения в боте.
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * Время создания токена.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // === Фабричный метод ===

    /**
     * Создаёт новый токен авторизации.
     *
     * @param token уникальный токен
     * @return новый TelegramAuthToken со статусом PENDING
     */
    public static TelegramAuthToken create(String token) {
        TelegramAuthToken authToken = new TelegramAuthToken();
        authToken.token = token;
        authToken.status = AuthTokenStatus.PENDING;
        authToken.expiresAt = Instant.now().plusSeconds(EXPIRATION_MINUTES * 60);
        authToken.createdAt = Instant.now();
        return authToken;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, истёк ли токен.
     *
     * @return true если токен истёк
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * Проверяет, находится ли токен в статусе PENDING.
     *
     * @return true если токен ожидает подтверждения
     */
    public boolean isPending() {
        return status == AuthTokenStatus.PENDING;
    }

    /**
     * Проверяет, подтверждён ли токен.
     *
     * @return true если токен подтверждён
     */
    public boolean isConfirmed() {
        return status == AuthTokenStatus.CONFIRMED;
    }

    /**
     * Проверяет, использован ли токен.
     *
     * @return true если токен уже использован
     */
    public boolean isUsed() {
        return status == AuthTokenStatus.USED;
    }

    /**
     * Проверяет, можно ли подтвердить токен (PENDING и не истёк).
     *
     * @return true если токен можно подтвердить
     */
    public boolean canBeConfirmed() {
        return isPending() && !isExpired();
    }

    /**
     * Проверяет, можно ли использовать токен для получения JWT (CONFIRMED и не истёк).
     *
     * @return true если токен можно использовать
     */
    public boolean canBeUsed() {
        return isConfirmed() && !isExpired();
    }

    /**
     * Подтверждает токен с данными пользователя из Telegram.
     *
     * @param telegramId    Telegram ID пользователя
     * @param firstName     имя
     * @param lastName      фамилия (может быть null)
     * @param username      username (может быть null)
     * @param chatId        chat ID для сообщений
     */
    public void confirm(String telegramId, String firstName, String lastName,
                        String username, String chatId) {
        this.telegramId = telegramId;
        this.telegramFirstName = firstName;
        this.telegramLastName = lastName;
        this.telegramUsername = username;
        this.telegramChatId = chatId;
        this.status = AuthTokenStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    /**
     * Помечает токен как использованный (JWT токены выданы).
     */
    public void markAsUsed() {
        this.status = AuthTokenStatus.USED;
    }

    /**
     * Помечает токен как истёкший.
     */
    public void markAsExpired() {
        this.status = AuthTokenStatus.EXPIRED;
    }
}
