package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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
import ru.aqstream.user.api.dto.ForgotPasswordRequest;
import ru.aqstream.user.api.dto.LoginRequest;
import ru.aqstream.user.api.dto.RefreshTokenRequest;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.api.dto.ResendVerificationRequest;
import ru.aqstream.user.api.dto.ResetPasswordRequest;
import ru.aqstream.user.api.dto.TelegramAuthRequest;
import ru.aqstream.user.api.dto.VerifyEmailRequest;
import ru.aqstream.user.service.AuthService;
import ru.aqstream.user.service.TelegramAuthService;
import ru.aqstream.user.service.VerificationService;
import ru.aqstream.user.util.CookieUtils;

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
    private final VerificationService verificationService;

    /**
     * Использовать Secure флаг для cookies (true для HTTPS в production).
     */
    @Value("${auth.cookie.secure:false}")
    private boolean secureCookie;

    @Operation(summary = "Регистрация", description = "Регистрация нового пользователя по email")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Пользователь зарегистрирован"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "409", description = "Email уже зарегистрирован")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = authService.register(request, userAgent, ipAddress);

        // Устанавливаем refresh token в httpOnly cookie
        CookieUtils.setRefreshTokenCookie(httpResponse, response.refreshToken(), secureCookie);

        // Возвращаем ответ без refreshToken в body (он в cookie)
        return ResponseEntity.status(HttpStatus.CREATED).body(response.withoutRefreshToken());
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
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = authService.login(request, userAgent, ipAddress);

        // Устанавливаем refresh token в httpOnly cookie
        CookieUtils.setRefreshTokenCookie(httpResponse, response.refreshToken(), secureCookie);

        return ResponseEntity.ok(response.withoutRefreshToken());
    }

    @Operation(summary = "Обновление токенов", description = "Обновление access и refresh токенов")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Токены обновлены"),
        @ApiResponse(responseCode = "401", description = "Невалидный refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
        @RequestBody(required = false) RefreshTokenRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        // Получаем refresh token из cookie или body (для обратной совместимости)
        String refreshToken = CookieUtils.getRefreshTokenFromCookie(httpRequest)
            .orElseGet(() -> request != null ? request.refreshToken() : null);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = authService.refresh(
            new RefreshTokenRequest(refreshToken), userAgent, ipAddress
        );

        // Устанавливаем новый refresh token в httpOnly cookie
        CookieUtils.setRefreshTokenCookie(httpResponse, response.refreshToken(), secureCookie);

        return ResponseEntity.ok(response.withoutRefreshToken());
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
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        AuthResponse response = telegramAuthService.authenticate(request, userAgent, ipAddress);

        // Устанавливаем refresh token в httpOnly cookie
        CookieUtils.setRefreshTokenCookie(httpResponse, response.refreshToken(), secureCookie);

        return ResponseEntity.ok(response.withoutRefreshToken());
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
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
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

        // Устанавливаем refresh token в httpOnly cookie
        CookieUtils.setRefreshTokenCookie(httpResponse, response.refreshToken(), secureCookie);

        return ResponseEntity.ok(response.withoutRefreshToken());
    }

    @Operation(
        summary = "Выход",
        description = "Отзывает все refresh токены пользователя (завершает все сессии)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Все токены отозваны"),
        @ApiResponse(responseCode = "401", description = "Невалидный refresh token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @RequestBody(required = false) RefreshTokenRequest request,
        HttpServletRequest httpRequest,
        HttpServletResponse httpResponse
    ) {
        // Получаем refresh token из cookie или body (для обратной совместимости)
        String refreshToken = CookieUtils.getRefreshTokenFromCookie(httpRequest)
            .orElseGet(() -> request != null ? request.refreshToken() : null);

        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        authService.logoutAll(refreshToken);

        // Удаляем cookie
        CookieUtils.clearRefreshTokenCookie(httpResponse, secureCookie);

        return ResponseEntity.noContent().build();
    }

    // === Email Verification ===

    @Operation(
        summary = "Подтверждение email",
        description = "Подтверждает email по токену из письма"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Email подтверждён"),
        @ApiResponse(responseCode = "400", description = "Недействительный или истёкший токен")
    })
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        verificationService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Повторная отправка письма верификации",
        description = "Отправляет повторное письмо для подтверждения email"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Письмо отправлено (если email существует)"),
        @ApiResponse(responseCode = "400", description = "Email уже подтверждён"),
        @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    })
    @PostMapping("/resend-verification")
    public ResponseEntity<Void> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        verificationService.resendVerificationEmail(request.email());
        return ResponseEntity.noContent().build();
    }

    // === Password Reset ===

    @Operation(
        summary = "Запрос сброса пароля",
        description = "Отправляет письмо со ссылкой для сброса пароля"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Письмо отправлено (если email существует)"),
        @ApiResponse(responseCode = "429", description = "Слишком много запросов")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        verificationService.requestPasswordReset(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Сброс пароля",
        description = "Устанавливает новый пароль по токену из письма. Завершает все активные сессии."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Пароль изменён, все сессии завершены"),
        @ApiResponse(responseCode = "400", description = "Недействительный токен или невалидный пароль")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        verificationService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
