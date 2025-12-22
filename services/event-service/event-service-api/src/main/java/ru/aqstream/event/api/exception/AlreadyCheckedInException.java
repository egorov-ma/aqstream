package ru.aqstream.event.api.exception;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import ru.aqstream.common.api.exception.ConflictException;

/**
 * Исключение для случаев, когда участник уже прошёл check-in.
 * Преобразуется в HTTP 409 Conflict.
 */
public class AlreadyCheckedInException extends ConflictException {

    /**
     * Создаёт исключение для повторного check-in.
     *
     * @param registrationId   идентификатор регистрации
     * @param confirmationCode код подтверждения
     * @param checkedInAt      время предыдущего check-in
     */
    public AlreadyCheckedInException(UUID registrationId, String confirmationCode, Instant checkedInAt) {
        super(
            "already_checked_in",
            "Участник уже прошёл check-in",
            Map.of(
                "registrationId", registrationId.toString(),
                "confirmationCode", confirmationCode,
                "checkedInAt", checkedInAt.toString()
            )
        );
    }
}
