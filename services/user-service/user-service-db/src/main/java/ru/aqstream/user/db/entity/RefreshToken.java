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

/**
 * Refresh токен для JWT аутентификации.
 *
 * <p>Токены хранятся в захешированном виде для безопасности.
 * Используется one-time use с rotation.</p>
 */
@Entity
@Table(name = "refresh_tokens", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    // === Фабричные методы ===

    /**
     * Создаёт новый refresh токен для пользователя.
     *
     * @param user      пользователь
     * @param tokenHash hash токена
     * @param expiresAt время истечения
     * @param userAgent User-Agent клиента
     * @param ipAddress IP адрес клиента
     * @return новый RefreshToken
     */
    public static RefreshToken create(User user, String tokenHash, Instant expiresAt,
                                      String userAgent, String ipAddress) {
        RefreshToken token = new RefreshToken();
        token.user = user;
        token.tokenHash = tokenHash;
        token.expiresAt = expiresAt;
        token.userAgent = userAgent;
        token.ipAddress = ipAddress;
        token.createdAt = Instant.now();
        return token;
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
     * Проверяет, валиден ли токен (не отозван и не истёк).
     *
     * @return true если токен валиден
     */
    public boolean isValid() {
        return !revoked && !isExpired();
    }

    /**
     * Отзывает токен.
     */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }
}
