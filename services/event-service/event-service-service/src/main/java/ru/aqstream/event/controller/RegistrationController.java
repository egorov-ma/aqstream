package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.api.PageResponse;
import ru.aqstream.common.api.exception.ForbiddenException;
import ru.aqstream.common.api.exception.UnauthorizedException;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.event.api.dto.CancelRegistrationRequest;
import ru.aqstream.event.api.dto.CreateRegistrationRequest;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.service.RegistrationService;

/**
 * Контроллер регистраций на события.
 * Эндпоинты для участников и организаторов.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Registrations", description = "Управление регистрациями на события")
public class RegistrationController {

    private final RegistrationService registrationService;

    // ==================== Для участников ====================

    @Operation(
        summary = "Зарегистрироваться на событие",
        description = "Создаёт регистрацию на событие. Для бесплатных билетов статус сразу CONFIRMED."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Регистрация создана"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Событие или тип билета не найдены"),
        @ApiResponse(responseCode = "409",
            description = "Регистрация закрыта, билеты распроданы или уже зарегистрирован")
    })
    @PostMapping("/events/{eventId}/registrations")
    public ResponseEntity<RegistrationDto> create(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Valid @RequestBody CreateRegistrationRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        RegistrationDto registration = registrationService.create(eventId, request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @Operation(
        summary = "Получить мои регистрации",
        description = "Возвращает список активных регистраций текущего пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список регистраций"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/registrations/my")
    public ResponseEntity<PageResponse<RegistrationDto>> getMyRegistrations(
        @AuthenticationPrincipal UserPrincipal principal,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        requireAuthenticated(principal);
        PageResponse<RegistrationDto> response = registrationService.getMyRegistrations(principal, pageable);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Получить регистрацию по ID",
        description = "Возвращает детали регистрации. Доступно владельцу или организатору."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Регистрация найдена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет доступа"),
        @ApiResponse(responseCode = "404", description = "Регистрация не найдена")
    })
    @GetMapping("/registrations/{id}")
    public ResponseEntity<RegistrationDto> getById(
        @Parameter(description = "ID регистрации")
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        RegistrationDto registration = registrationService.getById(id, principal);
        return ResponseEntity.ok(registration);
    }

    @Operation(
        summary = "Отменить регистрацию",
        description = "Отменяет регистрацию участником. Место возвращается в продажу."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Регистрация отменена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет доступа"),
        @ApiResponse(responseCode = "404", description = "Регистрация не найдена"),
        @ApiResponse(responseCode = "409", description = "Регистрация не может быть отменена")
    })
    @DeleteMapping("/registrations/{id}")
    public ResponseEntity<Void> cancel(
        @Parameter(description = "ID регистрации")
        @PathVariable UUID id,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        registrationService.cancel(id, principal);
        return ResponseEntity.noContent().build();
    }

    // ==================== Для организаторов ====================

    @Operation(
        summary = "Получить регистрации события",
        description = "Возвращает список регистраций события. Доступно только организаторам."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список регистраций"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Требуется роль организатора"),
        @ApiResponse(responseCode = "404", description = "Событие не найдено")
    })
    @GetMapping("/events/{eventId}/registrations")
    public ResponseEntity<PageResponse<RegistrationDto>> getEventRegistrations(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Parameter(description = "Фильтр по статусу")
        @RequestParam(required = false) RegistrationStatus status,
        @Parameter(description = "Фильтр по типу билета")
        @RequestParam(required = false) UUID ticketTypeId,
        @Parameter(description = "Поиск по имени/email")
        @RequestParam(required = false) String query,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireOrganizer(principal);

        PageResponse<RegistrationDto> response;
        if (query != null && !query.isBlank()) {
            response = registrationService.searchEventRegistrations(eventId, query, pageable);
        } else if (status != null) {
            response = registrationService.getEventRegistrationsByStatus(eventId, status, pageable);
        } else if (ticketTypeId != null) {
            response = registrationService.getEventRegistrationsByTicketType(eventId, ticketTypeId, pageable);
        } else {
            response = registrationService.getEventRegistrations(eventId, pageable);
        }

        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "Отменить регистрацию организатором",
        description = "Отменяет регистрацию с указанием причины. Доступно только организаторам."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Регистрация отменена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Требуется роль организатора"),
        @ApiResponse(responseCode = "404", description = "Событие или регистрация не найдены"),
        @ApiResponse(responseCode = "409", description = "Регистрация не может быть отменена")
    })
    @DeleteMapping("/events/{eventId}/registrations/{registrationId}")
    public ResponseEntity<Void> cancelByOrganizer(
        @Parameter(description = "ID события")
        @PathVariable UUID eventId,
        @Parameter(description = "ID регистрации")
        @PathVariable UUID registrationId,
        @RequestBody(required = false) CancelRegistrationRequest request,
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireOrganizer(principal);
        registrationService.cancelByOrganizer(eventId, registrationId, request);
        return ResponseEntity.noContent().build();
    }

    // ==================== Вспомогательные ====================

    /**
     * Проверяет, что пользователь аутентифицирован.
     */
    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("unauthorized", "Требуется аутентификация");
        }
    }

    /**
     * Проверяет, что пользователь является организатором.
     */
    private void requireOrganizer(UserPrincipal principal) {
        requireAuthenticated(principal);
        if (!principal.isOrganizer()) {
            throw new ForbiddenException("organizer_required", "Требуется роль организатора");
        }
    }
}
