package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие запроса подтверждения email.
 * Публикуется для отправки письма с ссылкой верификации.
 */
public class EmailVerificationRequestedEvent extends DomainEvent {

    private final UUID userId;
    private final String email;
    private final String verificationToken;
    private final String verificationUrl;

    /**
     * Создаёт событие запроса верификации email.
     *
     * @param userId            идентификатор пользователя
     * @param email             email для верификации
     * @param verificationToken токен верификации
     * @param verificationUrl   полная ссылка для верификации
     */
    public EmailVerificationRequestedEvent(UUID userId, String email, String verificationToken, String verificationUrl) {
        super();
        this.userId = userId;
        this.email = email;
        this.verificationToken = verificationToken;
        this.verificationUrl = verificationUrl;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    @Override
    public String getEventType() {
        return "user.email.verification.requested";
    }

    @Override
    public UUID getAggregateId() {
        return userId;
    }
}
