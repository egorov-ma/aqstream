package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.common.web.ClientIpResolver;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.LoginRequest;
import ru.aqstream.user.api.dto.RefreshTokenRequest;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.api.dto.TelegramAuthRequest;
import ru.aqstream.user.service.AuthService;
import ru.aqstream.user.service.TelegramAuthService;

/**
 * Контроллер аутентификации.
 * Регистрация, вход, обновление токенов, выход.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Регистрация, вход, обновление токенов")
public class AuthController {

    private final AuthService authService;
    private final TelegramAuthService telegramAuthService;

    @Operation(summary = "Регистрация", description = "Регистрация нового пользователя по email")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "409", description = "Email уже зарегистрирован")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = authService.register(request, userAgent, ipAddress);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Вход", description = "Вход по email и паролю")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешный вход"),
        @ApiResponse(responseCode = "401", description = "Неверные учётные данные"),
        @ApiResponse(responseCode = "403", description = "Аккаунт заблокирован")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = authService.login(request, userAgent, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Обновление токенов", description = "Обновление access и refresh токенов")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Токены обновлены"),
        @ApiResponse(responseCode = "401", description = "Невалидный refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @Valid @RequestBody RefreshTokenRequest request,
        HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = authService.refresh(request, userAgent, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Вход через Telegram",
        description = "Вход или регистрация через Telegram Login Widget. " +
            "При первом входе создаётся новый аккаунт с данными из Telegram."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешный вход/регистрация"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Невалидная подпись Telegram")
    })
    @PostMapping("/telegram")
    public ResponseEntity<AuthResponse> telegramAuth(
        @Valid @RequestBody TelegramAuthRequest request,
        HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = telegramAuthService.authenticate(request, userAgent, ipAddress);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Привязка Telegram к аккаунту",
        description = "Привязывает Telegram к существующему аккаунту пользователя. " +
            "Требует аутентификации. Telegram ID должен быть уникальным."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Telegram успешно привязан"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Невалидная подпись Telegram или не авторизован"),
        @ApiResponse(responseCode = "409", description = "Telegram уже привязан к другому аккаунту")
    })
    @PostMapping("/telegram/link")
    public ResponseEntity<AuthResponse> linkTelegram(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody TelegramAuthRequest request,
        HttpServletRequest httpRequest
    ) {
        // Проверяем что пользователь аутентифицирован (JWT передаётся через Gateway)
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = telegramAuthService.linkTelegram(
            principal.userId(), request, userAgent, ipAddress
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Выход", description = "Отзыв refresh token")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Токен отозван")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.revokeToken(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
