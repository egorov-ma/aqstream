package ru.aqstream.event.api.exception;

import ru.aqstream.common.api.exception.InternalServerException;

/**
 * Исключение при ошибке генерации QR-кода.
 * Преобразуется в HTTP 500 Internal Server Error.
 */
public class QrCodeGenerationException extends InternalServerException {

    /**
     * Создаёт исключение при ошибке генерации QR-кода.
     *
     * @param confirmationCode код подтверждения для которого генерировался QR
     * @param cause            причина ошибки
     */
    public QrCodeGenerationException(String confirmationCode, Throwable cause) {
        super(
            "qr_code_generation_error",
            "Ошибка генерации QR-кода",
            cause
        );
    }

    /**
     * Создаёт исключение при ошибке записи QR-кода.
     *
     * @param cause причина ошибки
     */
    public QrCodeGenerationException(Throwable cause) {
        super(
            "qr_code_generation_error",
            "Ошибка генерации QR-кода",
            cause
        );
    }
}
