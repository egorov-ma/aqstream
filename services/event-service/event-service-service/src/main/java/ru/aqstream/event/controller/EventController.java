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
import ru.aqstream.event.api.dto.CancelEventRequest;
import ru.aqstream.event.api.dto.CreateEventRequest;
import ru.aqstream.event.api.dto.EventAuditLogDto;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.UpdateEventRequest;
import ru.aqstream.event.service.EventAuditService;
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

    // ==================== CRUD ====================

    @Operation(
        summary = "Получить список событий",
        description = "Возвращает страницу событий организации. "
            + "Поддерживает фильтрацию по статусу, группе и диапазону дат."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список событий"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping
    public ResponseEntity<PageResponse<EventDto>> findAll(
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
        description = "Создаёт новое событие в статусе DRAFT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Событие создано"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "409", description = "Slug уже существует")
    })
    @PostMapping
    public ResponseEntity<EventDto> create(
        @Valid @RequestBody CreateEventRequest request
    ) {
        EventDto event = eventService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(event);
    }

    @Operation(
        summary = "Получить событие по ID",
        description = "Возвращает детали события."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие найдено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EventDto> getById(
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        EventDto event = eventService.getById(id);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Обновить событие",
        description = "Обновляет данные события. Все поля опциональны."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие обновлено"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @PutMapping("/{id}")
    public ResponseEntity<EventDto> update(
        @Parameter(description = "ID события")
        @PathVariable UUID id,
        @Valid @RequestBody UpdateEventRequest request
    ) {
        EventDto event = eventService.update(id, request);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Удалить событие",
        description = "Удаляет событие (soft delete)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Событие удалено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==================== Lifecycle ====================

    @Operation(
        summary = "Опубликовать событие",
        description = "Переводит событие из DRAFT в PUBLISHED. Нельзя опубликовать событие с датой в прошлом."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие опубликовано"),
        @ApiResponse(responseCode = "400", description = "Дата события в прошлом"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Некорректный переход статуса")
    })
    @PostMapping("/{id}/publish")
    public ResponseEntity<EventDto> publish(
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        EventDto event = eventService.publish(id);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Снять событие с публикации",
        description = "Переводит событие из PUBLISHED обратно в DRAFT."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие снято с публикации"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Некорректный переход статуса")
    })
    @PostMapping("/{id}/unpublish")
    public ResponseEntity<EventDto> unpublish(
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        EventDto event = eventService.unpublish(id);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Отменить событие",
        description = "Переводит событие в статус CANCELLED. Все регистрации отменяются."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие отменено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Событие уже отменено или завершено")
    })
    @PostMapping("/{id}/cancel")
    public ResponseEntity<EventDto> cancel(
        @Parameter(description = "ID события")
        @PathVariable UUID id,
        @Valid @RequestBody(required = false) CancelEventRequest request
    ) {
        String reason = request != null ? request.reason() : null;
        EventDto event = eventService.cancel(id, reason);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Завершить событие",
        description = "Переводит событие из PUBLISHED в COMPLETED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие завершено"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Некорректный переход статуса")
    })
    @PostMapping("/{id}/complete")
    public ResponseEntity<EventDto> complete(
        @Parameter(description = "ID события")
        @PathVariable UUID id
    ) {
        EventDto event = eventService.complete(id);
        return ResponseEntity.ok(event);
    }

    // ==================== Activity Log ====================

    @Operation(
        summary = "Получить историю изменений события",
        description = "Возвращает страницу записей аудита события (создание, обновление, публикация и т.д.)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "История изменений"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping("/{id}/activity")
    public ResponseEntity<PageResponse<EventAuditLogDto>> getActivity(
        @Parameter(description = "ID события")
        @PathVariable UUID id,
        @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<EventAuditLogDto> activity = eventAuditService.getEventHistory(id, pageable);
        return ResponseEntity.ok(activity);
    }
}
