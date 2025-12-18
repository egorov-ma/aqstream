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
import ru.aqstream.user.api.exception.TooManyVerificationRequestsException;
import ru.aqstream.user.api.exception.AccessDeniedException;
import ru.aqstream.user.api.exception.OrganizationRequestAlreadyReviewedException;
import ru.aqstream.user.api.exception.OrganizationRequestNotFoundException;
import ru.aqstream.user.api.exception.PendingRequestAlreadyExistsException;
import ru.aqstream.user.api.exception.SlugAlreadyExistsException;
import ru.aqstream.user.api.exception.UserNotFoundException;

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

    /**
     * Обрабатывает слишком много запросов на верификацию.
     * HTTP 429 Too Many Requests
     */
    @ExceptionHandler(TooManyVerificationRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyVerificationRequestsException ex) {
        return ResponseEntity
            .status(HttpStatus.TOO_MANY_REQUESTS)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    // === Organization Request Exceptions ===

    /**
     * Обрабатывает запрос на организацию не найден.
     * HTTP 404 Not Found
     */
    @ExceptionHandler(OrganizationRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationRequestNotFound(
        OrganizationRequestNotFoundException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает наличие активного запроса.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(PendingRequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handlePendingRequestAlreadyExists(
        PendingRequestAlreadyExistsException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает занятый slug.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(SlugAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleSlugAlreadyExists(SlugAlreadyExistsException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает уже рассмотренный запрос.
     * HTTP 409 Conflict
     */
    @ExceptionHandler(OrganizationRequestAlreadyReviewedException.class)
    public ResponseEntity<ErrorResponse> handleOrganizationRequestAlreadyReviewed(
        OrganizationRequestAlreadyReviewedException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает отказ в доступе.
     * HTTP 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }

    /**
     * Обрабатывает пользователь не найден.
     * HTTP 404 Not Found
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(ex.getCode(), ex.getMessage(), ex.getDetails()));
    }
}
