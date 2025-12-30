package ru.aqstream.notification.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.notification.api.dto.UserNotificationDto;
import ru.aqstream.notification.service.UserNotificationService;

/**
 * Контроллер для UI-уведомлений пользователя (bell icon).
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "User Notifications", description = "UI-уведомления пользователя")
public class UserNotificationController {

    private final UserNotificationService notificationService;

    @Operation(
        summary = "Получить уведомления",
        description = "Возвращает список уведомлений пользователя с пагинацией"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список уведомлений"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping
    public ResponseEntity<PageResponse<UserNotificationDto>> getNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "Номер страницы (0-based)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы")
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<UserNotificationDto> response = notificationService.getNotifications(
            principal.userId(), page, size
        );
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Получить количество непрочитанных",
        description = "Возвращает количество непрочитанных уведомлений для badge"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Количество непрочитанных"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        long count = notificationService.getUnreadCount(principal.userId());
        return ResponseEntity.ok(new UnreadCountResponse(count));
    }

    @Operation(
        summary = "Отметить уведомление как прочитанное",
        description = "Отмечает указанное уведомление как прочитанное"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Уведомление отмечено"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "ID уведомления")
            @PathVariable UUID id
    ) {
        notificationService.markAsRead(principal.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Отметить все как прочитанные",
        description = "Отмечает все уведомления пользователя как прочитанные"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Все уведомления отмечены"),
        @ApiResponse(responseCode = "401", description = "Требуется аутентификация")
    })
    @PostMapping("/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        int count = notificationService.markAllAsRead(principal.userId());
        return ResponseEntity.ok(new MarkAllReadResponse(count));
    }

    /**
     * Ответ с количеством непрочитанных.
     */
    public record UnreadCountResponse(long count) {
    }

    /**
     * Ответ с количеством отмеченных.
     */
    public record MarkAllReadResponse(int markedCount) {
    }
}
