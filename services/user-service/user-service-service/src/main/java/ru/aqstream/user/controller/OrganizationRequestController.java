package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.CreateOrganizationRequestRequest;
import ru.aqstream.user.api.dto.OrganizationRequestDto;
import ru.aqstream.user.api.dto.OrganizationRequestStatus;
import ru.aqstream.user.api.dto.RejectOrganizationRequestRequest;
import ru.aqstream.user.api.exception.AccessDeniedException;
import ru.aqstream.user.service.OrganizationRequestService;

/**
 * Контроллер запросов на создание организаций.
 */
@RestController
@RequestMapping("/api/v1/organization-requests")
@RequiredArgsConstructor
@Tag(name = "Organization Requests", description = "Запросы на создание организаций")
public class OrganizationRequestController {

    private final OrganizationRequestService requestService;

    @Operation(
        summary = "Подать запрос на создание организации",
        description = "Создаёт запрос на создание организации. Требует аутентификации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Запрос создан"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "409", description = "У пользователя уже есть активный запрос или slug занят")
    })
    @PostMapping
    public ResponseEntity<OrganizationRequestDto> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateOrganizationRequestRequest request
    ) {
        requireAuthenticated(principal);
        OrganizationRequestDto created = requestService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Получить запрос по ID",
        description = "Возвращает запрос по идентификатору. Пользователь видит только свои запросы, админ — все."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос найден"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
        @ApiResponse(responseCode = "404", description = "Запрос не найден")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationRequestDto> getById(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        requireAuthenticated(principal);
        OrganizationRequestDto request = requestService.getById(id, principal.userId(), principal.isAdmin());
        return ResponseEntity.ok(request);
    }

    @Operation(
        summary = "Получить свои запросы",
        description = "Возвращает все запросы текущего пользователя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список запросов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/my")
    public ResponseEntity<List<OrganizationRequestDto>> getMyRequests(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        List<OrganizationRequestDto> requests = requestService.getByUser(principal.userId());
        return ResponseEntity.ok(requests);
    }

    @Operation(
        summary = "Получить список запросов (админ)",
        description = "Возвращает список всех запросов с пагинацией. Только для администраторов."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Страница запросов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    @GetMapping
    public ResponseEntity<PageResponse<OrganizationRequestDto>> getAllRequests(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) OrganizationRequestStatus status
    ) {
        requireAdmin(principal);
        PageResponse<OrganizationRequestDto> requests = requestService.getAllRequests(page, size, status);
        return ResponseEntity.ok(requests);
    }

    @Operation(
        summary = "Получить pending запросы (админ)",
        description = "Возвращает список запросов, ожидающих рассмотрения. Только для администраторов."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Страница запросов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён")
    })
    @GetMapping("/pending")
    public ResponseEntity<PageResponse<OrganizationRequestDto>> getPendingRequests(
        @AuthenticationPrincipal UserPrincipal principal,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        requireAdmin(principal);
        PageResponse<OrganizationRequestDto> requests = requestService.getPendingRequests(page, size);
        return ResponseEntity.ok(requests);
    }

    @Operation(
        summary = "Одобрить запрос (админ)",
        description = "Одобряет запрос на создание организации. Только для администраторов."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос одобрен"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
        @ApiResponse(responseCode = "404", description = "Запрос не найден"),
        @ApiResponse(responseCode = "409", description = "Запрос уже рассмотрен")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<OrganizationRequestDto> approve(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        requireAdmin(principal);
        OrganizationRequestDto approved = requestService.approve(id, principal.userId());
        return ResponseEntity.ok(approved);
    }

    @Operation(
        summary = "Отклонить запрос (админ)",
        description = "Отклоняет запрос на создание организации с указанием причины. Только для администраторов."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Запрос отклонён"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Доступ запрещён"),
        @ApiResponse(responseCode = "404", description = "Запрос не найден"),
        @ApiResponse(responseCode = "409", description = "Запрос уже рассмотрен")
    })
    @PostMapping("/{id}/reject")
    public ResponseEntity<OrganizationRequestDto> reject(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody RejectOrganizationRequestRequest request
    ) {
        requireAdmin(principal);
        OrganizationRequestDto rejected = requestService.reject(id, principal.userId(), request);
        return ResponseEntity.ok(rejected);
    }

    // === Приватные методы ===

    /**
     * Проверяет, что пользователь аутентифицирован.
     *
     * @param principal данные пользователя
     * @throws AccessDeniedException если пользователь не аутентифицирован
     */
    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Требуется аутентификация");
        }
    }

    /**
     * Проверяет, что пользователь является администратором.
     *
     * @param principal данные пользователя
     * @throws AccessDeniedException если пользователь не админ
     */
    private void requireAdmin(UserPrincipal principal) {
        requireAuthenticated(principal);
        if (!principal.isAdmin()) {
            throw new AccessDeniedException("Требуются права администратора");
        }
    }
}
