package ru.aqstream.common.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Исключение для ошибок JWT аутентификации.
 * Наследуется от Spring Security AuthenticationException.
 */
public class JwtAuthenticationException extends AuthenticationException {

    public JwtAuthenticationException(String message) {
        super(message);
    }

    public JwtAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}
