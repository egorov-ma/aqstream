package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.user.api.dto.TelegramAuthInitResponse;
import ru.aqstream.user.api.dto.TelegramAuthStatusResponse;
import ru.aqstream.user.service.TelegramBotAuthService;

/**
 * Контроллер авторизации через Telegram бота.
 *
 * <p>Публичные endpoints для инициализации авторизации и проверки статуса.
 * Подтверждение авторизации выполняется через internal endpoint (вызывается ботом).</p>
 */
@RestController
@RequestMapping("/api/v1/auth/telegram")
@RequiredArgsConstructor
@Tag(name = "Telegram Bot Auth", description = "Авторизация через Telegram бота")
public class TelegramBotAuthController {

    private final TelegramBotAuthService telegramBotAuthService;

    @Operation(
        summary = "Инициализация авторизации",
        description = "Создаёт токен авторизации и возвращает deeplink на бота"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Токен создан"),
        @ApiResponse(responseCode = "500", description = "Внутренняя ошибка")
    })
    @PostMapping("/init")
    public ResponseEntity<TelegramAuthInitResponse> initAuth() {
        TelegramAuthInitResponse response = telegramBotAuthService.initAuth();
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Проверка статуса авторизации",
        description = "Возвращает текущий статус токена (PENDING, CONFIRMED, EXPIRED)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Статус получен"),
        @ApiResponse(responseCode = "404", description = "Токен не найден")
    })
    @GetMapping("/status/{token}")
    public ResponseEntity<TelegramAuthStatusResponse> checkStatus(@PathVariable String token) {
        TelegramAuthStatusResponse response = telegramBotAuthService.checkStatus(token);
        return ResponseEntity.ok(response);
    }
}
