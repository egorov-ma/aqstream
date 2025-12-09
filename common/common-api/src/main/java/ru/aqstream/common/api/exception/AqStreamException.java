package ru.aqstream.common.api.exception;

import java.util.Collections;
import java.util.Map;

/**
 * Базовое исключение для всех ошибок AqStream.
 * Все domain-specific исключения должны наследоваться от этого класса.
 */
public class AqStreamException extends RuntimeException {

    private final String code;
    private final Map<String, Object> details;

    /**
     * Создаёт исключение с кодом и сообщением.
     *
     * @param code    уникальный код ошибки
     * @param message сообщение об ошибке (на русском)
     */
    public AqStreamException(String code, String message) {
        super(message);
        this.code = code;
        this.details = Collections.emptyMap();
    }

    /**
     * Создаёт исключение с кодом, сообщением и деталями.
     *
     * @param code    уникальный код ошибки
     * @param message сообщение об ошибке (на русском)
     * @param details дополнительные детали ошибки
     */
    public AqStreamException(String code, String message, Map<String, Object> details) {
        super(message);
        this.code = code;
        this.details = details != null ? Map.copyOf(details) : Collections.emptyMap();
    }

    /**
     * Создаёт исключение с кодом, сообщением и причиной.
     *
     * @param code    уникальный код ошибки
     * @param message сообщение об ошибке (на русском)
     * @param cause   причина исключения
     */
    public AqStreamException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.details = Collections.emptyMap();
    }

    /**
     * Код ошибки для API response.
     *
     * @return код ошибки
     */
    public String getCode() {
        return code;
    }

    /**
     * Дополнительные детали ошибки.
     *
     * @return неизменяемая карта деталей
     */
    public Map<String, Object> getDetails() {
        return details;
    }
}
