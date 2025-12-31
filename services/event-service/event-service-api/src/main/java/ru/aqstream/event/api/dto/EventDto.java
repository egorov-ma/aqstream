package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO события для API ответов.
 *
 * @param id                       идентификатор события
 * @param tenantId                 идентификатор организации
 * @param organizerName            название организации (для публичной страницы)
 * @param title                    название события
 * @param slug                     URL-slug события
 * @param description              описание в формате Markdown
 * @param status                   статус события
 * @param startsAt                 дата и время начала
 * @param endsAt                   дата и время окончания
 * @param timezone                 часовой пояс
 * @param locationType             тип локации
 * @param locationAddress          физический адрес
 * @param onlineUrl                ссылка на онлайн-площадку
 * @param maxCapacity              максимальное количество участников
 * @param registrationOpensAt      дата открытия регистрации
 * @param registrationClosesAt     дата закрытия регистрации
 * @param isPublic                 публичность события
 * @param participantsVisibility   видимость списка участников
 * @param groupId                  ID группы для приватных событий
 * @param registrationFormConfig   конфигурация формы регистрации
 * @param cancelReason             причина отмены события
 * @param cancelledAt              дата отмены события
 * @param recurrenceRule           правило повторения (если это повторяющееся событие)
 * @param parentEventId            ID родительского события (для экземпляров серии)
 * @param instanceDate             дата экземпляра серии
 * @param createdAt                дата создания
 * @param updatedAt                дата обновления
 */
public record EventDto(
    UUID id,
    UUID tenantId,
    String organizerName,
    String title,
    String slug,
    String description,
    EventStatus status,
    Instant startsAt,
    Instant endsAt,
    String timezone,
    LocationType locationType,
    String locationAddress,
    String onlineUrl,
    Integer maxCapacity,
    Instant registrationOpensAt,
    Instant registrationClosesAt,
    boolean isPublic,
    ParticipantsVisibility participantsVisibility,
    UUID groupId,
    RegistrationFormConfig registrationFormConfig,
    String cancelReason,
    Instant cancelledAt,
    RecurrenceRuleDto recurrenceRule,
    UUID parentEventId,
    LocalDate instanceDate,
    Instant createdAt,
    Instant updatedAt
) {

    /**
     * Проверяет, является ли событие повторяющимся (шаблоном серии).
     *
     * @return true если у события есть правило повторения
     */
    public boolean isRecurring() {
        return recurrenceRule != null;
    }

    /**
     * Проверяет, является ли событие экземпляром серии.
     *
     * @return true если событие имеет родительское событие
     */
    public boolean isSeriesInstance() {
        return parentEventId != null;
    }
}
