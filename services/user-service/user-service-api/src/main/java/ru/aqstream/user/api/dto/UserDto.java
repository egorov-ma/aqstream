package ru.aqstream.user.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO пользователя.
 *
 * @param id            идентификатор пользователя
 * @param email         email пользователя
 * @param firstName     имя
 * @param lastName      фамилия
 * @param avatarUrl     URL аватара
 * @param emailVerified подтверждён ли email
 * @param createdAt     дата регистрации
 */
public record UserDto(
    UUID id,
    String email,
    String firstName,
    String lastName,
    String avatarUrl,
    boolean emailVerified,
    Instant createdAt
) {
}
