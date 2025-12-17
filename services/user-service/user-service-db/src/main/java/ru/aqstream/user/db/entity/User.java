package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.SQLRestriction;
import ru.aqstream.common.data.BaseEntity;

/**
 * Сущность пользователя.
 *
 * <p>Пользователь может быть членом нескольких организаций,
 * поэтому НЕ наследуется от TenantAwareEntity.</p>
 *
 * <p>Поддерживает soft delete через поле deletedAt.</p>
 */
@Entity
@Table(name = "users", schema = "user_service")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"passwordHash"})
public class User extends BaseEntity {

    /**
     * Максимальное количество неудачных попыток входа до блокировки.
     */
    public static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    /**
     * Длительность блокировки аккаунта в минутах.
     */
    public static final int LOCK_DURATION_MINUTES = 15;

    @Column(name = "telegram_id", length = 64)
    private String telegramId;

    @Column(name = "telegram_chat_id", length = 64)
    private String telegramChatId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // === Фабричные методы ===

    /**
     * Создаёт пользователя с email регистрацией.
     *
     * @param email        email пользователя
     * @param passwordHash bcrypt hash пароля
     * @param firstName    имя
     * @param lastName     фамилия
     * @return новый пользователь
     */
    public static User createWithEmail(String email, String passwordHash, String firstName, String lastName) {
        User user = new User();
        user.email = normalizeEmail(email);
        user.passwordHash = passwordHash;
        user.firstName = firstName;
        user.lastName = lastName;
        user.emailVerified = false;
        return user;
    }

    /**
     * Создаёт пользователя с Telegram регистрацией.
     *
     * @param telegramId     Telegram ID пользователя
     * @param telegramChatId Telegram Chat ID для уведомлений
     * @param firstName      имя
     * @param lastName       фамилия
     * @param avatarUrl      URL фото из Telegram (nullable)
     * @return новый пользователь
     */
    public static User createWithTelegram(
        String telegramId,
        String telegramChatId,
        String firstName,
        String lastName,
        String avatarUrl
    ) {
        User user = new User();
        user.telegramId = telegramId;
        user.telegramChatId = telegramChatId;
        user.firstName = firstName;
        user.lastName = lastName;
        user.avatarUrl = avatarUrl;
        // Email не подтверждён, т.к. при Telegram-регистрации email отсутствует.
        // Аккаунт активен — верификация email не требуется для Telegram-пользователей.
        user.emailVerified = false;
        return user;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, удалён ли пользователь.
     *
     * @return true если пользователь удалён
     */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * Помечает пользователя как удалённого (soft delete).
     */
    public void softDelete() {
        this.deletedAt = Instant.now();
    }

    /**
     * Восстанавливает удалённого пользователя.
     */
    public void restore() {
        this.deletedAt = null;
    }

    /**
     * Проверяет, заблокирован ли аккаунт.
     *
     * @return true если аккаунт заблокирован
     */
    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(Instant.now());
    }

    /**
     * Регистрирует неудачную попытку входа.
     * После {@link #MAX_FAILED_LOGIN_ATTEMPTS} неудачных попыток блокирует аккаунт
     * на {@link #LOCK_DURATION_MINUTES} минут.
     */
    public void recordFailedLogin() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            this.lockedUntil = Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60L);
        }
    }

    /**
     * Регистрирует успешный вход.
     * Сбрасывает счётчик неудачных попыток и снимает блокировку.
     */
    public void recordSuccessfulLogin() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
        this.lastLoginAt = Instant.now();
    }

    /**
     * Подтверждает email пользователя.
     */
    public void verifyEmail() {
        this.emailVerified = true;
        this.emailVerifiedAt = Instant.now();
    }

    /**
     * Устанавливает email с нормализацией (lowercase).
     *
     * @param email email пользователя
     */
    public void setEmail(String email) {
        this.email = normalizeEmail(email);
    }

    /**
     * Нормализует email (приводит к нижнему регистру).
     *
     * @param email исходный email
     * @return нормализованный email
     */
    private static String normalizeEmail(String email) {
        return email != null ? email.toLowerCase().trim() : null;
    }
}
