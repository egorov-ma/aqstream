package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие запроса сброса пароля.
 * Публикуется для отправки письма с ссылкой сброса.
 */
public class PasswordResetRequestedEvent extends DomainEvent {

    private final UUID userId;
    private final String email;
    private final String resetToken;
    private final String resetUrl;

    /**
     * Создаёт событие запроса сброса пароля.
     *
     * @param userId     идентификатор пользователя
     * @param email      email пользователя
     * @param resetToken токен сброса
     * @param resetUrl   полная ссылка для сброса
     */
    public PasswordResetRequestedEvent(UUID userId, String email, String resetToken, String resetUrl) {
        super();
        this.userId = userId;
        this.email = email;
        this.resetToken = resetToken;
        this.resetUrl = resetUrl;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getResetToken() {
        return resetToken;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    @Override
    public String getEventType() {
        return "user.password.reset.requested";
    }

    @Override
    public UUID getAggregateId() {
        return userId;
    }
}
