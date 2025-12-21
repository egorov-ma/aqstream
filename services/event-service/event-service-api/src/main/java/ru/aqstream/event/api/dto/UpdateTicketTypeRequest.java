package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Запрос на обновление типа билета.
 *
 * <p>Все поля опциональны — обновляются только переданные.
 *
 * @param name        название типа билета (произвольное от организатора)
 * @param description описание типа билета
 * @param quantity    количество билетов (null = unlimited)
 * @param salesStart  начало продаж
 * @param salesEnd    окончание продаж
 * @param sortOrder   порядок сортировки
 * @param isActive    активен ли тип билета
 */
public record UpdateTicketTypeRequest(
    @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
    String name,

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    String description,

    @Min(value = 1, message = "Количество должно быть не менее 1")
    Integer quantity,

    Instant salesStart,

    Instant salesEnd,

    @Min(value = 0, message = "Порядок сортировки не может быть отрицательным")
    Integer sortOrder,

    Boolean isActive
) {
}
