package ru.aqstream.event.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO типа билета для API ответов.
 *
 * @param id           идентификатор типа билета
 * @param eventId      идентификатор события
 * @param name         название типа билета (заданное организатором)
 * @param description  описание типа билета
 * @param priceCents   цена в копейках (0 = бесплатный, Phase 2)
 * @param currency     валюта
 * @param quantity     общее количество (null = unlimited)
 * @param soldCount    количество проданных
 * @param reservedCount количество зарезервированных
 * @param available    доступно для продажи
 * @param salesStart   начало продаж
 * @param salesEnd     окончание продаж
 * @param sortOrder    порядок сортировки
 * @param isActive     активен ли тип билета
 * @param isSoldOut    распроданы ли билеты
 * @param createdAt    дата создания
 * @param updatedAt    дата обновления
 */
public record TicketTypeDto(
    UUID id,
    UUID eventId,
    String name,
    String description,
    int priceCents,
    String currency,
    Integer quantity,
    int soldCount,
    int reservedCount,
    Integer available,
    Instant salesStart,
    Instant salesEnd,
    int sortOrder,
    boolean isActive,
    boolean isSoldOut,
    Instant createdAt,
    Instant updatedAt
) {
}
