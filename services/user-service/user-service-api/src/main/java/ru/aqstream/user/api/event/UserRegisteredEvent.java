package ru.aqstream.user.api.event;

import java.util.UUID;
import ru.aqstream.common.api.event.DomainEvent;

/**
 * Событие регистрации нового пользователя.
 * Публикуется при успешной регистрации через email или Telegram.
 * Используется для отправки приветственного уведомления.
 */
public class UserRegisteredEvent extends DomainEvent {

    private final UUID userId;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String telegramChatId;
    private final RegistrationSource source;

    /**
     * Источник регистрации.
     */
    public enum RegistrationSource {
        EMAIL,
        TELEGRAM
    }

    /**
     * Создаёт событие регистрации пользователя.
     *
     * @param userId         идентификатор пользователя
     * @param firstName      имя пользователя
     * @param lastName       фамилия пользователя (может быть null)
     * @param email          email (null для Telegram-only пользователей)
     * @param telegramChatId ID чата Telegram (null для email-only пользователей)
     * @param source         источник регистрации
     */
    public UserRegisteredEvent(
        UUID userId,
        String firstName,
        String lastName,
        String email,
        String telegramChatId,
        RegistrationSource source
    ) {
        super();
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.telegramChatId = telegramChatId;
        this.source = source;
    }

    /**
     * Создаёт событие для регистрации через email.
     */
    public static UserRegisteredEvent forEmail(UUID userId, String firstName, String lastName, String email) {
        return new UserRegisteredEvent(userId, firstName, lastName, email, null, RegistrationSource.EMAIL);
    }

    /**
     * Создаёт событие для регистрации через Telegram.
     */
    public static UserRegisteredEvent forTelegram(
        UUID userId,
        String firstName,
        String lastName,
        String telegramChatId
    ) {
        return new UserRegisteredEvent(userId, firstName, lastName, null, telegramChatId, RegistrationSource.TELEGRAM);
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getTelegramChatId() {
        return telegramChatId;
    }

    public RegistrationSource getSource() {
        return source;
    }

    @Override
    public String getEventType() {
        return "user.registered";
    }

    @Override
    public UUID getAggregateId() {
        return userId;
    }
}
