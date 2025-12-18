package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Токен верификации email или сброса пароля.
 *
 * <p>Токены одноразовые — после использования помечаются в поле used_at.
 * Истёкшие и использованные токены периодически удаляются.</p>
 */
@Entity
@Table(name = "verification_tokens", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"user", "token"})
public class VerificationToken {

    /**
     * Время жизни токена верификации email — 24 часа.
     */
    public static final long EMAIL_VERIFICATION_EXPIRATION_HOURS = 24;

    /**
     * Время жизни токена сброса пароля — 1 час.
     */
    public static final long PASSWORD_RESET_EXPIRATION_HOURS = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Тип токена верификации.
     */
    public enum TokenType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }

    // === Фабричные методы ===

    /**
     * Создаёт токен для верификации email.
     *
     * @param user  пользователь
     * @param token уникальный токен
     * @return новый VerificationToken
     */
    public static VerificationToken createEmailVerification(User user, String token) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.user = user;
        verificationToken.token = token;
        verificationToken.type = TokenType.EMAIL_VERIFICATION;
        verificationToken.expiresAt = Instant.now().plusSeconds(EMAIL_VERIFICATION_EXPIRATION_HOURS * 3600);
        verificationToken.createdAt = Instant.now();
        return verificationToken;
    }

    /**
     * Создаёт токен для сброса пароля.
     *
     * @param user  пользователь
     * @param token уникальный токен
     * @return новый VerificationToken
     */
    public static VerificationToken createPasswordReset(User user, String token) {
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.user = user;
        verificationToken.token = token;
        verificationToken.type = TokenType.PASSWORD_RESET;
        verificationToken.expiresAt = Instant.now().plusSeconds(PASSWORD_RESET_EXPIRATION_HOURS * 3600);
        verificationToken.createdAt = Instant.now();
        return verificationToken;
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
