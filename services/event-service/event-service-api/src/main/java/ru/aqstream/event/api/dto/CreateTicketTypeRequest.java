package ru.aqstream.event.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Запрос на создание типа билета.
 *
 * <p>В Phase 2 все билеты бесплатные, поэтому price_cents всегда 0.
 *
 * @param name        название типа билета (произвольное от организатора, обязательно)
 * @param description описание типа билета (опционально)
 * @param quantity    количество билетов (null = unlimited)
 * @param salesStart  начало продаж (опционально)
 * @param salesEnd    окончание продаж (опционально)
 * @param sortOrder   порядок сортировки (по умолчанию 0)
 */
public record CreateTicketTypeRequest(
    @NotBlank(message = "Название типа билета обязательно")
    @Size(min = 1, max = 100, message = "Название должно быть от 1 до 100 символов")
    String name,

    @Size(max = 1000, message = "Описание не должно превышать 1000 символов")
    String description,

    @Min(value = 1, message = "Количество должно быть не менее 1")
    Integer quantity,

    Instant salesStart,

    Instant salesEnd,

    @Min(value = 0, message = "Порядок сортировки не может быть отрицательным")
    Integer sortOrder
) {

    /**
     * Возвращает порядок сортировки с дефолтным значением.
     *
     * @return порядок сортировки
     */
    public int sortOrderOrDefault() {
        return sortOrder != null ? sortOrder : 0;
    }
}
