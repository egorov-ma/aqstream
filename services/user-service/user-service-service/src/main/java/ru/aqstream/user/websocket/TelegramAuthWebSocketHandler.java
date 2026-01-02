package ru.aqstream.user.websocket;

import static ru.aqstream.user.api.util.TelegramUtils.maskToken;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import ru.aqstream.user.api.dto.AuthResponse;

/**
 * WebSocket handler для real-time уведомлений об авторизации через Telegram бота.
 *
 * <p>Frontend подключается к /ws/telegram-auth/{token} после инициализации авторизации.
 * После подтверждения в боте, этот handler отправляет JWT токены через WebSocket.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /**
     * Map: token -> WebSocket session.
     */
    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String token = extractToken(session);
        if (token != null) {
            sessions.put(token, session);
            log.info("WebSocket подключён для авторизации: token={}", maskToken(token));
        } else {
            log.warn("WebSocket подключение без токена, закрываем");
            closeQuietly(session);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String token = extractToken(session);
        if (token != null) {
            sessions.remove(token);
            log.debug("WebSocket закрыт: token={}, status={}", maskToken(token), status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String token = extractToken(session);
        log.error("WebSocket ошибка: token={}, error={}",
            maskToken(token),
            exception.getMessage());
        closeQuietly(session);
    }

    /**
     * Уведомляет frontend о подтверждении авторизации.
     * Отправляет JWT токены через WebSocket и закрывает соединение.
     *
     * @param token        токен авторизации
     * @param authResponse данные авторизации с JWT
     */
    public void notifyConfirmation(String token, AuthResponse authResponse) {
        WebSocketSession session = sessions.get(token);
        if (session != null && session.isOpen()) {
            try {
                // Формируем сообщение
                Map<String, Object> message = Map.of(
                    "type", "confirmed",
                    "accessToken", authResponse.accessToken(),
                    "refreshToken", authResponse.refreshToken() != null ? authResponse.refreshToken() : "",
                    "expiresIn", authResponse.expiresIn(),
                    "tokenType", authResponse.tokenType(),
                    "user", authResponse.user()
                );

                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));

                log.info("WebSocket: отправлено подтверждение авторизации: token={}", maskToken(token));

                // Закрываем соединение после успешной отправки
                session.close(CloseStatus.NORMAL);
                sessions.remove(token);

            } catch (IOException e) {
                log.error("Ошибка отправки WebSocket сообщения: token={}, error={}",
                    maskToken(token), e.getMessage());
                closeQuietly(session);
            }
        } else {
            log.debug("WebSocket сессия не найдена или закрыта: token={}", maskToken(token));
        }
    }

    /**
     * Извлекает токен из URI WebSocket сессии.
     * URI формат: /ws/telegram-auth/{token}
     */
    private String extractToken(WebSocketSession session) {
        if (session.getUri() == null) {
            return null;
        }
        String path = session.getUri().getPath();
        if (path == null || !path.contains("/ws/telegram-auth/")) {
            return null;
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return null;
    }

    /**
     * Закрывает WebSocket сессию без выброса исключений.
     */
    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (IOException e) {
            log.debug("Ошибка при закрытии WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Возвращает количество активных сессий.
     * Используется для мониторинга.
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}
