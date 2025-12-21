package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.event.api.dto.CreateTicketTypeRequest;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.api.dto.UpdateTicketTypeRequest;
import ru.aqstream.event.service.TicketTypeService;

/**
 * Контроллер типов билетов.
 * CRUD операции для типов билетов события.
 */
@RestController
@RequestMapping("/api/v1/events/{eventId}/ticket-types")
@RequiredArgsConstructor
@Tag(name = "Ticket Types", description = "Управление типами билетов")
public class TicketTypeController {

    private final TicketTypeService ticketTypeService;

    // ==================== CRUD ====================

    @Operation(
        summary = "Получить список типов билетов",
        description = "Возвращает все типы билетов события (включая неактивные для организатора)."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список типов билетов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping
    public ResponseEntity<List<TicketTypeDto>> findAll(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId
    ) {
        List<TicketTypeDto> ticketTypes = ticketTypeService.findAllByEventId(eventId);
        return ResponseEntity.ok(ticketTypes);
    }

    @Operation(
        summary = "Создать тип билета",
        description = "Создаёт новый тип билета для события. В Phase 2 все билеты бесплатные."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Тип билета создан"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Событие нельзя редактировать")
    })
    @PostMapping
    public ResponseEntity<TicketTypeDto> create(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Valid @RequestBody CreateTicketTypeRequest request
    ) {
        TicketTypeDto ticketType = ticketTypeService.create(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketType);
    }

    @Operation(
        summary = "Получить тип билета по ID",
        description = "Возвращает детали типа билета."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тип билета найден"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Тип билета или событие не найдено")
    })
    @GetMapping("/{ticketTypeId}")
    public ResponseEntity<TicketTypeDto> getById(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Parameter(description = "ID типа билета")
        @PathVariable UUID ticketTypeId
    ) {
        TicketTypeDto ticketType = ticketTypeService.getById(eventId, ticketTypeId);
        return ResponseEntity.ok(ticketType);
    }

    @Operation(
        summary = "Обновить тип билета",
        description = "Обновляет данные типа билета. Все поля опциональны."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тип билета обновлён"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Тип билета или событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Событие нельзя редактировать")
    })
    @PutMapping("/{ticketTypeId}")
    public ResponseEntity<TicketTypeDto> update(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Parameter(description = "ID типа билета")
        @PathVariable UUID ticketTypeId,
        @Valid @RequestBody UpdateTicketTypeRequest request
    ) {
        TicketTypeDto ticketType = ticketTypeService.update(eventId, ticketTypeId, request);
        return ResponseEntity.ok(ticketType);
    }

    @Operation(
        summary = "Удалить тип билета",
        description = "Удаляет тип билета. Невозможно удалить тип с регистрациями — используйте деактивацию."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Тип билета удалён"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Тип билета или событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Есть регистрации или событие нельзя редактировать")
    })
    @DeleteMapping("/{ticketTypeId}")
    public ResponseEntity<Void> delete(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Parameter(description = "ID типа билета")
        @PathVariable UUID ticketTypeId
    ) {
        ticketTypeService.delete(eventId, ticketTypeId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Деактивировать тип билета",
        description = "Деактивирует тип билета вместо удаления. Деактивированный тип не отображается при регистрации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Тип билета деактивирован"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Тип билета или событие не найдено"),
        @ApiResponse(responseCode = "409", description = "Событие нельзя редактировать")
    })
    @PostMapping("/{ticketTypeId}/deactivate")
    public ResponseEntity<TicketTypeDto> deactivate(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Parameter(description = "ID типа билета")
        @PathVariable UUID ticketTypeId
    ) {
        TicketTypeDto ticketType = ticketTypeService.deactivate(eventId, ticketTypeId);
        return ResponseEntity.ok(ticketType);
    }
}
