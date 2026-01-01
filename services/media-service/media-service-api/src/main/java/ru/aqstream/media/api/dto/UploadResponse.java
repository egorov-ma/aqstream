package ru.aqstream.media.api.dto;

import java.util.UUID;

/**
 * Ответ на загрузку файла.
 *
 * @param id          идентификатор файла
 * @param url         публичный URL для доступа к файлу
 * @param originalName оригинальное имя файла
 * @param contentType MIME-тип файла
 * @param fileSize    размер файла в байтах
 */
public record UploadResponse(
    UUID id,
    String url,
    String originalName,
    String contentType,
    long fileSize
) {
}
