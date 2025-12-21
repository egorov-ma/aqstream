package ru.aqstream.event.api.dto;

/**
 * Тип локации события.
 */
public enum LocationType {

    /**
     * Онлайн — виртуальное мероприятие (zoom, teams, etc.).
     * Требуется online_url.
     */
    ONLINE,

    /**
     * Оффлайн — физическое мероприятие.
     * Требуется location_address.
     */
    OFFLINE,

    /**
     * Гибридное — онлайн + оффлайн одновременно.
     * Могут быть указаны оба: location_address и online_url.
     */
    HYBRID
}
