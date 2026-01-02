package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.api.exception.UnauthorizedException;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.PublicEventSummaryDto;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.TicketTypeDto;
import ru.aqstream.event.service.EventService;
import ru.aqstream.event.service.RegistrationService;
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

    private static final int MAX_PAGE_SIZE = 50;

    private final EventService eventService;
    private final TicketTypeService ticketTypeService;
    private final RegistrationService registrationService;

    @Operation(
        summary = "Получить список предстоящих публичных событий",
        description = "Возвращает пагинированный список публичных опубликованных событий, "
            + "отсортированных по дате начала (ближайшие первые). Не требует авторизации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список событий")
    })
    @GetMapping
    public ResponseEntity<PageResponse<PublicEventSummaryDto>> listUpcoming(
        @Parameter(description = "Номер страницы (0-based)")
        @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "Размер страницы (макс 50)")
        @RequestParam(defaultValue = "12") int size
    ) {
        // Ограничиваем размер страницы для защиты от abuse
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, safeSize);

        PageResponse<PublicEventSummaryDto> events = eventService.findUpcomingPublicEvents(pageable);
        return ResponseEntity.ok(events);
    }

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

    @Operation(
        summary = "Зарегистрироваться на публичное событие",
        description = "Создаёт регистрацию на публичное событие. Требует аутентификации. "
            + "Для бесплатных билетов статус сразу CONFIRMED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Регистрация создана"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие или тип билета не найдены"),
        @ApiResponse(responseCode = "409",
            description = "Регистрация закрыта, билеты распроданы или уже зарегистрирован")
    })
    @PostMapping("/{slug}/registrations")
    public ResponseEntity<RegistrationDto> createRegistration(
        @Parameter(description = "URL-slug события")
        @PathVariable String slug,
        @Valid @RequestBody CreateRegistrationRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        if (principal == null) {
            throw new UnauthorizedException("unauthorized", "Требуется аутентификация");
        }
        RegistrationDto registration = registrationService.createForPublicEvent(slug, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }
}
