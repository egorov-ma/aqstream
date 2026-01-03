package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.api.exception.ForbiddenException;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.CancelEventRequest;
import ru.aqstream.event.api.dto.CreateEventRequest;
import ru.aqstream.event.api.dto.EventAuditLogDto;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.service.EventAuditService;
import ru.aqstream.event.service.EventPermissionService;
import ru.aqstream.event.service.EventService;

/**
 * Контроллер событий.
 * CRUD операции и управление жизненным циклом.
 */
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Управление событиями")
public class EventController {

    private final EventService eventService;
    private final EventAuditService eventAuditService;
    private final EventPermissionService eventPermissionService;

    // ==================== CRUD ====================

    @Operation(
        summary = "Получить список событий",
        description = "Возвращает страницу событий организации. "
            + "Поддерживает фильтрацию по статусу, группе и диапазону дат. "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список событий"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для просмотра событий")
    })
    @GetMapping
    public ResponseEntity<PageResponse<EventDto>> findAll(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "Фильтр по статусу")
        @RequestParam(required = false) EventStatus status,
        @Parameter(description = "Фильтр по группе")
        @RequestParam(required = false) UUID groupId,
        @Parameter(description = "Фильтр по дате начала (от), ISO 8601 формат")
        @RequestParam(required = false) Instant startsAfter,
        @Parameter(description = "Фильтр по дате начала (до), ISO 8601 формат")
        @RequestParam(required = false) Instant startsBefore,
        @PageableDefault(size = 20, sort = "startsAt") Pageable pageable
    ) {
        // Проверяем право на просмотр событий в dashboard
        eventPermissionService.validateViewPermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        PageResponse<EventDto> response;
        if (startsAfter != null && startsBefore != null) {
            response = eventService.findByDateRange(startsAfter, startsBefore, pageable);
        } else if (status != null) {
            response = eventService.findByStatus(status, pageable);
        } else if (groupId != null) {
            response = eventService.findByGroup(groupId, pageable);
        } else {
            response = eventService.findAll(pageable);
        }
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Создать событие",
        description = "Создаёт новое событие в статусе DRAFT. "
            + "Для создания необходимо быть OWNER или MODERATOR организации. "
            + "Админ платформы может указать organizationId для создания в любой организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Событие создано"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для создания события"),
        @ApiResponse(responseCode = "409", description = "Slug уже существует")
    })
    @PostMapping
    public ResponseEntity<EventDto> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateEventRequest request
    ) {
        boolean isAdmin = principal.roles().contains("ADMIN");

        // Определяем целевую организацию
        UUID targetOrganizationId = (request.organizationId() != null && isAdmin)
            ? request.organizationId()
            : principal.tenantId();

        if (targetOrganizationId == null) {
            throw new ForbiddenException("Выберите организацию для создания события");
        }

        // Проверяем права на создание события
        eventPermissionService.validateCreatePermission(
            principal.userId(),
            targetOrganizationId,
            isAdmin
        );

        EventDto event = eventService.create(request, targetOrganizationId);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @Operation(
        summary = "Получить событие по ID",
        description = "Возвращает детали события. Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие найдено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для просмотра события"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getById(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        // Проверяем право на просмотр события в dashboard
        eventPermissionService.validateViewPermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        EventDto event = eventService.getById(id);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Обновить событие",
        description = "Обновляет данные события. Все поля опциональны. "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие обновлено"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для обновления события"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EventDto> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id,
        @Valid @RequestBody UpdateEventRequest request
    ) {
        // Проверяем право на управление событием
        eventPermissionService.validateManagePermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        EventDto event = eventService.update(id, request);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Удалить событие",
        description = "Удаляет событие (soft delete). Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Событие удалено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для удаления события"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        // Проверяем право на управление событием
        eventPermissionService.validateManagePermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Lifecycle ====================

    @Operation(
        summary = "Опубликовать событие",
        description = "Переводит событие из DRAFT в PUBLISHED. Нельзя опубликовать событие с датой в прошлом. "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие опубликовано"),
        @ApiResponse(responseCode = "400", description = "Дата события в прошлом"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для публикации события"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Некорректный переход статуса")
    })
    @PostMapping("/{id}/publish")
    public ResponseEntity<EventDto> publish(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        // Проверяем право на управление событием
        eventPermissionService.validateManagePermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        EventDto event = eventService.publish(id);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Снять событие с публикации",
        description = "Переводит событие из PUBLISHED обратно в DRAFT. "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие снято с публикации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для снятия события с публикации"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Некорректный переход статуса")
    })
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<EventDto> unpublish(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        // Проверяем право на управление событием
        eventPermissionService.validateManagePermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        EventDto event = eventService.unpublish(id);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Отменить событие",
        description = "Переводит событие в статус CANCELLED. Все регистрации отменяются. "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие отменено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для отмены события"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Событие уже отменено или завершено")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventDto> cancel(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) CancelEventRequest request
    ) {
        // Проверяем право на управление событием
        eventPermissionService.validateManagePermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        String reason = request != null ? request.reason() : null;
        EventDto event = eventService.cancel(id, reason);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Завершить событие",
        description = "Переводит событие из PUBLISHED в COMPLETED. "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие завершено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для завершения события"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Некорректный переход статуса")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<EventDto> complete(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        // Проверяем право на управление событием
        eventPermissionService.validateManagePermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        EventDto event = eventService.complete(id);
        return ResponseEntity.ok(event);
    }

    // ==================== Activity Log ====================

    @Operation(
        summary = "Получить историю изменений события",
        description = "Возвращает страницу записей аудита события (создание, обновление, публикация и т.д.). "
            + "Доступно только для OWNER и MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "История изменений"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав для просмотра истории"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping("/{id}/activity")
    public ResponseEntity<PageResponse<EventAuditLogDto>> getActivity(
        @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "ID события")
        @PathVariable UUID id,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        // Проверяем право на просмотр события в dashboard
        eventPermissionService.validateViewPermission(
            principal.userId(),
            principal.tenantId(),
            principal.roles().contains("ADMIN")
        );

        PageResponse<EventAuditLogDto> activity = eventAuditService.getEventHistory(id, pageable);
        return ResponseEntity.ok(activity);
    }
}
