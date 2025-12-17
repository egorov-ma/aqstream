package ru.aqstream.user.api.exception;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import ru.aqstream.common.api.exception.AqStreamException;

/**
 * Исключение: аккаунт заблокирован после нескольких неудачных попыток входа.
 * HTTP статус: 403 Forbidden
 */
public class AccountLockedException extends AqStreamException {

    private static final String CODE = "account_locked";

    public AccountLockedException(Instant lockedUntil) {
        super(CODE, formatMessage(lockedUntil), createDetails(lockedUntil));
    }

    private static String formatMessage(Instant lockedUntil) {
        if (lockedUntil == null) {
            return "Аккаунт временно заблокирован из-за множества неудачных попыток входа";
        }

        Duration remaining = Duration.between(Instant.now(), lockedUntil);
        if (remaining.isNegative()) {
            return "Аккаунт временно заблокирован";
        }

        long minutes = remaining.toMinutes();
        if (minutes < 1) {
            return "Аккаунт заблокирован. Попробуйте через несколько секунд";
        } else if (minutes == 1) {
            return "Аккаунт заблокирован. Попробуйте через 1 минуту";
        } else {
            return "Аккаунт заблокирован. Попробуйте через " + minutes + " минут";
        }
    }

    private static Map<String, Object> createDetails(Instant lockedUntil) {
        if (lockedUntil == null) {
            return Map.of();
        }
        return Map.of("lockedUntil", lockedUntil.toString());
    }
}
