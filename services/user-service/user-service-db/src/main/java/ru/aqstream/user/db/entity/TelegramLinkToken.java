package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Токен для привязки Telegram аккаунта к существующему email-аккаунту.
 *
 * <p>Токены одноразовые — после использования помечаются в поле used_at.
 * Время жизни токена — 15 минут.
 * Истёкшие и использованные токены периодически удаляются.</p>
 */
@Entity
@Table(name = "telegram_link_tokens", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user", "token"})
public class TelegramLinkToken {

    /**
     * Время жизни токена — 15 минут.
     */
    public static final long EXPIRATION_MINUTES = 15;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 64, unique = true)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // === Фабричный метод ===

    /**
     * Создаёт токен для привязки Telegram.
     *
     * @param user  пользователь, к которому привязывается Telegram
     * @param token уникальный токен
     * @return новый TelegramLinkToken
     */
    public static TelegramLinkToken create(User user, String token) {
        TelegramLinkToken linkToken = new TelegramLinkToken();
        linkToken.user = user;
        linkToken.token = token;
        linkToken.expiresAt = Instant.now().plusSeconds(EXPIRATION_MINUTES * 60);
        linkToken.createdAt = Instant.now();
        return linkToken;
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
     * Проверяет, использован ли токен.
     *
     * @return true если токен уже использован
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Проверяет, валиден ли токен (не использован и не истёк).
     *
     * @return true если токен валиден
     */
    public boolean isValid() {
        return !isUsed() && !isExpired();
    }

    /**
     * Помечает токен как использованный.
     */
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }
}
