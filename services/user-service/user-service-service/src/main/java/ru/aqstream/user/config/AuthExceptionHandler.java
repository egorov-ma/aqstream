package ru.aqstream.user.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.aqstream.common.api.ErrorResponse;
import ru.aqstream.user.api.exception.AccountLockedException;
import ru.aqstream.user.api.exception.InvalidCredentialsException;
import ru.aqstream.user.api.exception.InvalidTelegramAuthException;
import ru.aqstream.user.api.exception.TelegramIdAlreadyExistsException;

/**
 * Обработчик исключений аутентификации.
 * Приоритет выше GlobalExceptionHandler для перехвата специфичных исключений.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthExceptionHandler {

    /**
     * Обрабатывает неверные учётные данные.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает заблокированный аккаунт.
     * HTTP 403 Forbidden
     */
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(AccountLockedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает невалидную Telegram аутентификацию.
     * HTTP 401 Unauthorized
     */
    @ExceptionHandler(InvalidTelegramAuthException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTelegramAuth(InvalidTelegramAuthException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает конфликт Telegram ID.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(TelegramIdAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleTelegramIdAlreadyExists(TelegramIdAlreadyExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }
}
