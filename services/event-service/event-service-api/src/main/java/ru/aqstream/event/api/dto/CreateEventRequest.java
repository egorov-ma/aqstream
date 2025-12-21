package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Запрос на создание события.
 *
 * @param title                  название события (обязательно)
 * @param description            описание в формате Markdown (опционально)
 * @param startsAt               дата и время начала (обязательно)
 * @param endsAt                 дата и время окончания (опционально)
 * @param timezone               часовой пояс (по умолчанию Europe/Moscow)
 * @param locationType           тип локации (по умолчанию ONLINE)
 * @param locationAddress        физический адрес (для OFFLINE/HYBRID)
 * @param onlineUrl              ссылка на онлайн-площадку (для ONLINE/HYBRID)
 * @param maxCapacity            максимальное количество участников (опционально)
 * @param registrationOpensAt    дата открытия регистрации (опционально)
 * @param registrationClosesAt   дата закрытия регистрации (опционально)
 * @param isPublic               публичность события (по умолчанию false)
 * @param participantsVisibility видимость списка участников (по умолчанию CLOSED)
 * @param groupId                ID группы для приватных событий (опционально)
 */
public record CreateEventRequest(
    @NotBlank(message = "Название события обязательно")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    String title,

    @Size(max = 10000, message = "Описание не должно превышать 10000 символов")
    String description,

    @NotNull(message = "Дата начала обязательна")
    Instant startsAt,

    Instant endsAt,

    @Size(max = 50, message = "Часовой пояс не должен превышать 50 символов")
    String timezone,

    LocationType locationType,

    @Size(max = 500, message = "Адрес не должен превышать 500 символов")
    String locationAddress,

    @Size(max = 500, message = "URL не должен превышать 500 символов")
    String onlineUrl,

    Integer maxCapacity,

    Instant registrationOpensAt,

    Instant registrationClosesAt,

    Boolean isPublic,

    ParticipantsVisibility participantsVisibility,

    UUID groupId
) {

    /**
     * Возвращает часовой пояс с дефолтным значением.
     *
     * @return часовой пояс
     */
    public String timezoneOrDefault() {
        return timezone != null ? timezone : "Europe/Moscow";
    }

    /**
     * Возвращает тип локации с дефолтным значением.
     *
     * @return тип локации
     */
    public LocationType locationTypeOrDefault() {
        return locationType != null ? locationType : LocationType.ONLINE;
    }

    /**
     * Возвращает флаг публичности с дефолтным значением.
     *
     * @return публичность
     */
    public boolean isPublicOrDefault() {
        return isPublic != null && isPublic;
    }

    /**
     * Возвращает видимость участников с дефолтным значением.
     *
     * @return видимость участников
     */
    public ParticipantsVisibility participantsVisibilityOrDefault() {
        return participantsVisibility != null ? participantsVisibility : ParticipantsVisibility.CLOSED;
    }
}
