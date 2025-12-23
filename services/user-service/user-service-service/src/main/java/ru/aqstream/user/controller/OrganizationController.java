package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.CreateOrganizationRequest;
import ru.aqstream.user.api.dto.InviteMemberRequest;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.api.dto.OrganizationInviteDto;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.UpdateMemberRoleRequest;
import ru.aqstream.user.api.dto.UpdateOrganizationRequest;
import ru.aqstream.user.api.exception.AccessDeniedException;
import ru.aqstream.user.service.OrganizationInviteService;
import ru.aqstream.user.service.OrganizationService;

/**
 * Контроллер организаций.
 * CRUD организаций, управление членами, приглашения, переключение.
 */
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Управление организациями")
public class OrganizationController {

    private final OrganizationService organizationService;
    private final OrganizationInviteService inviteService;

    // ==================== CRUD Организаций ====================

    @Operation(
        summary = "Получить список своих организаций",
        description = "Возвращает список организаций, в которых пользователь является членом."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список организаций"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping
    public ResponseEntity<List<OrganizationDto>> getMyOrganizations(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        List<OrganizationDto> organizations = organizationService.getMyOrganizations(principal.userId());
        return ResponseEntity.ok(organizations);
    }

    @Operation(
        summary = "Создать организацию",
        description = "Создаёт организацию. Требуется одобренный запрос на создание организации. "
            + "Slug берётся автоматически из одобренного запроса."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Организация создана"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "409", description = "Нет одобренного запроса или slug уже занят")
    })
    @PostMapping
    public ResponseEntity<OrganizationDto> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @Valid @RequestBody CreateOrganizationRequest request
    ) {
        requireAuthenticated(principal);
        OrganizationDto created = organizationService.create(principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Получить организацию по ID",
        description = "Возвращает детали организации. Доступно только членам организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Организация найдена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Не член организации"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @GetMapping("/{id}")
    public ResponseEntity<OrganizationDto> getById(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        requireAuthenticated(principal);
        OrganizationDto organization = organizationService.getById(id, principal.userId());
        return ResponseEntity.ok(organization);
    }

    @Operation(
        summary = "Обновить организацию",
        description = "Обновляет данные организации. Доступно OWNER и MODERATOR."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Организация обновлена"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @PutMapping("/{id}")
    public ResponseEntity<OrganizationDto> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        requireAuthenticated(principal);
        OrganizationDto updated = organizationService.update(id, principal.userId(), request);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Удалить организацию",
        description = "Удаляет организацию (soft delete). Доступно только OWNER."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Организация удалена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только OWNER)"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        requireAuthenticated(principal);
        organizationService.delete(id, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ==================== Переключение организаций ====================

    @Operation(
        summary = "Переключиться на организацию",
        description = "Переключает контекст на другую организацию. "
            + "Возвращает новые токены с tenantId = organizationId."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Переключение выполнено, возвращены новые токены"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Не член организации"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @PostMapping("/{id}/switch")
    public ResponseEntity<AuthResponse> switchOrganization(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        HttpServletRequest httpRequest
    ) {
        requireAuthenticated(principal);
        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = getClientIp(httpRequest);
        AuthResponse response = organizationService.switchOrganization(
            principal.userId(), id, userAgent, ipAddress
        );
        return ResponseEntity.ok(response);
    }

    // ==================== Управление членами ====================

    @Operation(
        summary = "Получить список членов организации",
        description = "Возвращает список всех членов организации. Доступно членам организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список членов"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Не член организации"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @GetMapping("/{id}/members")
    public ResponseEntity<List<OrganizationMemberDto>> getMembers(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        requireAuthenticated(principal);
        List<OrganizationMemberDto> members = organizationService.getMembers(id, principal.userId());
        return ResponseEntity.ok(members);
    }

    @Operation(
        summary = "Изменить роль члена организации",
        description = "Изменяет роль члена организации. Доступно только OWNER."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Роль изменена"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (только OWNER)"),
        @ApiResponse(responseCode = "404", description = "Организация или член не найдены")
    })
    @PutMapping("/{id}/members/{userId}")
    public ResponseEntity<OrganizationMemberDto> updateMemberRole(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @PathVariable UUID userId,
        @Valid @RequestBody UpdateMemberRoleRequest request
    ) {
        requireAuthenticated(principal);
        OrganizationMemberDto member = organizationService.updateMemberRole(
            id, principal.userId(), userId, request
        );
        return ResponseEntity.ok(member);
    }

    @Operation(
        summary = "Удалить члена из организации",
        description = "Удаляет члена из организации. OWNER и MODERATOR могут удалять. "
            + "OWNER не может быть удалён."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Член удалён"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав или попытка удалить OWNER"),
        @ApiResponse(responseCode = "404", description = "Организация или член не найдены")
    })
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @PathVariable UUID userId
    ) {
        requireAuthenticated(principal);
        organizationService.removeMember(id, principal.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Приглашения ====================

    @Operation(
        summary = "Создать приглашение в организацию",
        description = "Создаёт приглашение для нового члена. Возвращает Telegram deeplink. "
            + "Доступно OWNER и MODERATOR. Приглашение действительно 7 дней."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Приглашение создано"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @PostMapping("/{id}/invite")
    public ResponseEntity<OrganizationInviteDto> createInvite(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id,
        @Valid @RequestBody InviteMemberRequest request
    ) {
        requireAuthenticated(principal);
        OrganizationInviteDto invite = inviteService.createInvite(id, principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(invite);
    }

    @Operation(
        summary = "Получить активные приглашения",
        description = "Возвращает список активных (не использованных и не истёкших) приглашений. "
            + "Доступно членам организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список приглашений"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Не член организации"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @GetMapping("/{id}/invites")
    public ResponseEntity<List<OrganizationInviteDto>> getActiveInvites(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID id
    ) {
        requireAuthenticated(principal);
        List<OrganizationInviteDto> invites = inviteService.getActiveInvites(id, principal.userId());
        return ResponseEntity.ok(invites);
    }

    @Operation(
        summary = "Принять приглашение",
        description = "Принимает приглашение по коду. Пользователь становится MODERATOR организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Приглашение принято, пользователь добавлен в организацию"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Приглашение не найдено"),
        @ApiResponse(responseCode = "409",
            description = "Приглашение истекло, уже использовано или пользователь уже член")
    })
    @PostMapping("/join/{inviteCode}")
    public ResponseEntity<OrganizationMemberDto> acceptInvite(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable String inviteCode
    ) {
        requireAuthenticated(principal);
        OrganizationMemberDto member = inviteService.acceptInvite(principal.userId(), inviteCode);
        return ResponseEntity.ok(member);
    }

    // ==================== Вспомогательные методы ====================

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
     * Получает IP адрес клиента.
     *
     * @param request HTTP запрос
     * @return IP адрес
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
