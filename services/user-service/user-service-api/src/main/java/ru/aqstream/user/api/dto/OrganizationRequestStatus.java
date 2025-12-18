package ru.aqstream.user.api.dto;

/**
 * Статус запроса на создание организации.
 */
public enum OrganizationRequestStatus {

    /**
     * Ожидает рассмотрения.
     */
    PENDING,

    /**
     * Одобрен.
     */
    APPROVED,

    /**
     * Отклонён.
     */
    REJECTED
}
