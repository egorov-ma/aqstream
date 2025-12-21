package ru.aqstream.user.api.exception;

import java.util.Map;
import ru.aqstream.common.api.exception.TooManyRequestsException;

/**
 * Исключение: слишком много запросов на верификацию.
 * HTTP статус: 429 Too Many Requests
 *
 * <p>Используется для rate limiting запросов на отправку писем
 * подтверждения email и сброса пароля.</p>
 */
public class TooManyVerificationRequestsException extends TooManyRequestsException {

    private static final String CODE = "too_many_requests";
    private static final String MESSAGE = "Слишком много запросов. Попробуйте через %d минут";

    public TooManyVerificationRequestsException(long retryAfterMinutes) {
        super(
            CODE,
            String.format(MESSAGE, retryAfterMinutes),
            Map.of("retryAfterMinutes", retryAfterMinutes)
        );
    }
}
