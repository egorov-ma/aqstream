package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

/**
 * Запрос на обновление события.
 * Все поля опциональны — обновляются только переданные.
 *
 * @param title                  название события
 * @param description            описание в формате Markdown
 * @param startsAt               дата и время начала
 * @param endsAt                 дата и время окончания
 * @param timezone               часовой пояс
 * @param locationType           тип локации
 * @param locationAddress        физический адрес
 * @param onlineUrl              ссылка на онлайн-площадку
 * @param maxCapacity            максимальное количество участников
 * @param registrationOpensAt    дата открытия регистрации
 * @param registrationClosesAt   дата закрытия регистрации
 * @param isPublic               публичность события
 * @param participantsVisibility видимость списка участников
 * @param groupId                ID группы для приватных событий
 */
public record UpdateEventRequest(
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    String title,

    @Size(max = 10000, message = "Описание не должно превышать 10000 символов")
    String description,

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
}
