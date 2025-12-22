package ru.aqstream.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.notification.api.dto.NotificationPreferencesDto;
import ru.aqstream.notification.api.dto.UpdatePreferencesRequest;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.service.PreferenceService;

/**
 * Контроллер для управления настройками уведомлений пользователя.
 */
@RestController
@RequestMapping("/api/v1/notifications/preferences")
@RequiredArgsConstructor
@Tag(name = "Notification Preferences", description = "Управление настройками уведомлений")
public class PreferencesController {

    private final PreferenceService preferenceService;

    @Operation(summary = "Получить настройки", description = "Возвращает текущие настройки уведомлений пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Настройки получены"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping
    public ResponseEntity<NotificationPreferencesDto> getPreferences(
            @AuthenticationPrincipal UserPrincipal principal) {
        NotificationPreference pref = preferenceService.getPreferences(principal.userId());
        return ResponseEntity.ok(new NotificationPreferencesDto(pref.getSettings()));
    }

    @Operation(summary = "Обновить настройки", description = "Обновляет настройки уведомлений пользователя")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Настройки обновлены"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @PutMapping
    public ResponseEntity<NotificationPreferencesDto> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdatePreferencesRequest request) {
        NotificationPreference pref = preferenceService.updatePreferences(
            principal.userId(),
            request.settings()
        );
        return ResponseEntity.ok(new NotificationPreferencesDto(pref.getSettings()));
    }
}
