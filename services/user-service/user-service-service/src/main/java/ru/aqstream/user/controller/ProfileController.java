package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.ChangePasswordRequest;
import ru.aqstream.user.api.dto.TelegramLinkTokenResponse;
import ru.aqstream.user.api.dto.UpdateProfileRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.service.ProfileService;
import ru.aqstream.user.service.TelegramLinkService;

/**
 * Контроллер управления профилем пользователя.
 * Обновление личных данных, смена пароля и привязка Telegram.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Управление профилем пользователя")
public class ProfileController {

    private final ProfileService profileService;
    private final TelegramLinkService telegramLinkService;

    @Value("${telegram.bot.username:AqStreamBot}")
    private String telegramBotUsername;

    @Operation(
        summary = "Обновить профиль",
        description = "Обновляет личные данные пользователя (имя, фамилия)"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Профиль обновлён"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PatchMapping("/users/me")
    public ResponseEntity<UserDto> updateProfile(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        UserDto updatedUser = profileService.updateProfile(principal.userId(), request);
        return ResponseEntity.ok(updatedUser);
    }

    @Operation(
        summary = "Сменить пароль",
        description = "Изменяет пароль пользователя. Требует указания текущего пароля для подтверждения."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Пароль изменён"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные или слабый пароль"),
        @ApiResponse(responseCode = "401", description = "Не авторизован или неверный текущий пароль"),
        @ApiResponse(responseCode = "404", description = "Пользователь не найден")
    })
    @PostMapping("/auth/change-password")
    public ResponseEntity<Void> changePassword(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        profileService.changePassword(principal.userId(), request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Создать токен для привязки Telegram",
        description = "Генерирует токен и ссылку на бота для привязки Telegram к аккаунту"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Токен создан"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @PostMapping("/users/me/telegram/link-token")
    public ResponseEntity<TelegramLinkTokenResponse> generateTelegramLinkToken(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = telegramLinkService.createLinkToken(principal.userId());
        String botLink = "https://t.me/" + telegramBotUsername + "?start=link_" + token;

        return ResponseEntity.ok(new TelegramLinkTokenResponse(token, botLink));
    }
}
