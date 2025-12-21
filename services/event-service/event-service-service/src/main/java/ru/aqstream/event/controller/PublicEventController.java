package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.service.EventService;
import ru.aqstream.event.service.TicketTypeService;

/**
 * Публичный контроллер событий.
 * Доступ без авторизации к публичным событиям.
 */
@RestController
@RequestMapping("/api/v1/public/events")
@RequiredArgsConstructor
@Tag(name = "Public Events", description = "Публичный доступ к событиям")
public class PublicEventController {

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;

    @Operation(
        summary = "Получить публичное событие по slug",
        description = "Возвращает публичное опубликованное событие. Не требует авторизации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Событие найдено"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено или не публичное")
    })
    @GetMapping("/{slug}")
    public ResponseEntity<EventDto> getBySlug(
        @Parameter(description = "URL-slug события")
        @PathVariable String slug
    ) {
        EventDto event = eventService.getPublicBySlug(slug);
        return ResponseEntity.ok(event);
    }

    @Operation(
        summary = "Получить типы билетов публичного события",
        description = "Возвращает активные типы билетов в период продаж. Не требует авторизации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список типов билетов"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено или не публичное")
    })
    @GetMapping("/{slug}/ticket-types")
    public ResponseEntity<List<TicketTypeDto>> getTicketTypes(
        @Parameter(description = "URL-slug события")
        @PathVariable String slug
    ) {
        List<TicketTypeDto> ticketTypes = ticketTypeService.findPublicByEventSlug(slug);
        return ResponseEntity.ok(ticketTypes);
    }
}
