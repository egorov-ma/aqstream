package ru.aqstream.user.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.aqstream.common.data.BaseEntity;
import ru.aqstream.user.api.dto.OrganizationRole;

/**
 * Приглашение в организацию.
 *
 * <p>Содержит код приглашения и срок действия.
 * Приглашённый пользователь принимает приглашение по коду.</p>
 */
@Entity
@Table(name = "organization_invites", schema = "user_service")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrganizationInvite extends BaseEntity {

    /**
     * Длина кода приглашения.
     */
    public static final int INVITE_CODE_LENGTH = 32;

    /**
     * Срок действия приглашения в днях.
     */
    public static final int INVITE_EXPIRATION_DAYS = 7;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "invite_code", nullable = false, unique = true, length = 32)
    private String inviteCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy;

    @Column(name = "telegram_username", length = 100)
    private String telegramUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private OrganizationRole role = OrganizationRole.MODERATOR;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "used_by")
    private User usedBy;

    // === Фабричные методы ===

    /**
     * Создаёт новое приглашение.
     *
     * @param organization     организация
     * @param invitedBy        кто приглашает
     * @param telegramUsername Telegram username приглашённого (nullable)
     * @return новое приглашение
     */
    public static OrganizationInvite create(
        Organization organization,
        User invitedBy,
        String telegramUsername
    ) {
        OrganizationInvite invite = new OrganizationInvite();
        invite.organization = organization;
        invite.invitedBy = invitedBy;
        invite.telegramUsername = telegramUsername;
        invite.role = OrganizationRole.MODERATOR;
        invite.inviteCode = generateInviteCode();
        invite.expiresAt = Instant.now().plusSeconds(INVITE_EXPIRATION_DAYS * 24 * 60 * 60L);
        return invite;
    }

    // === Бизнес-методы ===

    /**
     * Проверяет, истекло ли приглашение.
     *
     * @return true если приглашение истекло
     */
    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    /**
     * Проверяет, использовано ли приглашение.
     *
     * @return true если приглашение уже использовано
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Проверяет, действительно ли приглашение.
     *
     * @return true если приглашение можно использовать
     */
    public boolean isValid() {
        return !isExpired() && !isUsed();
    }

    /**
     * Отмечает приглашение как использованное.
     *
     * @param user пользователь, принявший приглашение
     */
    public void markAsUsed(User user) {
        this.usedAt = Instant.now();
        this.usedBy = user;
    }

    /**
     * Возвращает ID организации.
     *
     * @return UUID организации
     */
    public UUID getOrganizationId() {
        return organization != null ? organization.getId() : null;
    }

    /**
     * Возвращает ID пригласившего.
     *
     * @return UUID пригласившего
     */
    public UUID getInvitedById() {
        return invitedBy != null ? invitedBy.getId() : null;
    }

    /**
     * Возвращает ID пользователя, использовавшего приглашение.
     *
     * @return UUID пользователя или null
     */
    public UUID getUsedById() {
        return usedBy != null ? usedBy.getId() : null;
    }

    /**
     * Генерирует случайный код приглашения.
     *
     * @return код приглашения
     */
    private static String generateInviteCode() {
        StringBuilder sb = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(SECURE_RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
