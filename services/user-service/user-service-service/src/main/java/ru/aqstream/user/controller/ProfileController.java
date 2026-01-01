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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.ChangePasswordRequest;
import ru.aqstream.user.api.dto.UpdateProfileRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.service.ProfileService;

/**
 * Контроллер управления профилем пользователя.
 * Обновление личных данных и смена пароля.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Управление профилем пользователя")
public class ProfileController {

    private final ProfileService profileService;

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
}
