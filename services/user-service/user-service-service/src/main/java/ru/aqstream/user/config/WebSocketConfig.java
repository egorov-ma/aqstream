package ru.aqstream.user.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import ru.aqstream.user.websocket.TelegramAuthWebSocketHandler;

/**
 * Конфигурация WebSocket для авторизации через Telegram бота.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final TelegramAuthWebSocketHandler telegramAuthWebSocketHandler;

    @Value("${websocket.allowed-origins:*}")
    private String allowedOrigins;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(telegramAuthWebSocketHandler, "/ws/telegram-auth/*")
            .setAllowedOrigins(allowedOrigins.split(","));
    }
}
