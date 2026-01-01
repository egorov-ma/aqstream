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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.CreateGroupRequest;
import ru.aqstream.user.api.dto.GroupDto;
import ru.aqstream.user.api.dto.GroupMemberDto;
import ru.aqstream.user.api.dto.JoinGroupResponse;
import ru.aqstream.user.api.dto.UpdateGroupRequest;
import ru.aqstream.user.api.exception.AccessDeniedException;
import ru.aqstream.user.service.GroupService;

/**
 * Контроллер групп.
 * CRUD групп, управление участниками, присоединение по коду.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Groups", description = "Управление группами для приватных событий")
public class GroupController {

    private final GroupService groupService;

    // ==================== Мои группы ====================

    @Operation(
        summary = "Получить мои группы",
        description = "Возвращает список групп, в которых текущий пользователь является участником."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список групп"),
        @ApiResponse(responseCode = "401", description = "Не авторизован")
    })
    @GetMapping("/groups/my")
    public ResponseEntity<List<GroupDto>> getMyGroups(
        @AuthenticationPrincipal UserPrincipal principal
    ) {
        requireAuthenticated(principal);
        List<GroupDto> groups = groupService.getMyGroups(principal.userId());
        return ResponseEntity.ok(groups);
    }

    // ==================== CRUD Групп ====================

    @Operation(
        summary = "Получить группы организации",
        description = "Возвращает список групп организации. "
            + "OWNER/MODERATOR видят все группы, обычный участник — только свои."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список групп"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Не член организации"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @GetMapping("/organizations/{organizationId}/groups")
    public ResponseEntity<List<GroupDto>> getByOrganization(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId
    ) {
        requireAuthenticated(principal);
        List<GroupDto> groups = groupService.getGroupsByOrganization(organizationId, principal.userId());
        return ResponseEntity.ok(groups);
    }

    @Operation(
        summary = "Создать группу",
        description = "Создаёт группу внутри организации. Доступно OWNER и MODERATOR. "
            + "Создатель автоматически становится участником."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Группа создана"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется MODERATOR)"),
        @ApiResponse(responseCode = "404", description = "Организация не найдена")
    })
    @PostMapping("/organizations/{organizationId}/groups")
    public ResponseEntity<GroupDto> create(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID organizationId,
        @Valid @RequestBody CreateGroupRequest request
    ) {
        requireAuthenticated(principal);
        GroupDto created = groupService.create(organizationId, principal.userId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(
        summary = "Получить группу по ID",
        description = "Возвращает детали группы. "
            + "Доступно OWNER/MODERATOR организации или участникам группы."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Группа найдена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет доступа к группе"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<GroupDto> getById(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId
    ) {
        requireAuthenticated(principal);
        GroupDto group = groupService.getById(groupId, principal.userId());
        return ResponseEntity.ok(group);
    }

    @Operation(
        summary = "Обновить группу",
        description = "Обновляет название и описание группы. Доступно OWNER и MODERATOR."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Группа обновлена"),
        @ApiResponse(responseCode = "400", description = "Невалидные данные"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<GroupDto> update(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId,
        @Valid @RequestBody UpdateGroupRequest request
    ) {
        requireAuthenticated(principal);
        GroupDto updated = groupService.update(groupId, principal.userId(), request);
        return ResponseEntity.ok(updated);
    }

    @Operation(
        summary = "Удалить группу",
        description = "Удаляет группу. Доступно только OWNER организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Группа удалена"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав (требуется OWNER)"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId
    ) {
        requireAuthenticated(principal);
        groupService.delete(groupId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Регенерировать инвайт-код",
        description = "Генерирует новый уникальный инвайт-код для группы. Доступно OWNER и MODERATOR."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Код регенерирован"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @PostMapping("/groups/{groupId}/regenerate-code")
    public ResponseEntity<GroupDto> regenerateCode(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId
    ) {
        requireAuthenticated(principal);
        GroupDto updated = groupService.regenerateInviteCode(groupId, principal.userId());
        return ResponseEntity.ok(updated);
    }

    // ==================== Управление участниками ====================

    @Operation(
        summary = "Получить участников группы",
        description = "Возвращает список участников группы. "
            + "Доступно OWNER/MODERATOR организации или участникам группы."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Список участников"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Нет доступа к группе"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена")
    })
    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<List<GroupMemberDto>> getMembers(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId
    ) {
        requireAuthenticated(principal);
        List<GroupMemberDto> members = groupService.getMembers(groupId, principal.userId());
        return ResponseEntity.ok(members);
    }

    @Operation(
        summary = "Удалить участника из группы",
        description = "Удаляет участника из группы. Доступно OWNER и MODERATOR. "
            + "Создателя группы удалить нельзя."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Участник удалён"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Недостаточно прав"),
        @ApiResponse(responseCode = "404", description = "Группа или участник не найдены"),
        @ApiResponse(responseCode = "409", description = "Нельзя удалить создателя группы")
    })
    @DeleteMapping("/groups/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId,
        @PathVariable UUID userId
    ) {
        requireAuthenticated(principal);
        groupService.removeMember(groupId, principal.userId(), userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Выйти из группы",
        description = "Участник покидает группу. Создатель группы не может выйти."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Успешный выход"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена или не участник"),
        @ApiResponse(responseCode = "409", description = "Создатель не может выйти из группы")
    })
    @PostMapping("/groups/{groupId}/leave")
    public ResponseEntity<Void> leave(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable UUID groupId
    ) {
        requireAuthenticated(principal);
        groupService.leave(groupId, principal.userId());
        return ResponseEntity.noContent().build();
    }

    // ==================== Присоединение по коду ====================

    @Operation(
        summary = "Присоединиться к группе по инвайт-коду",
        description = "Присоединяет пользователя к группе по инвайт-коду. "
            + "Пользователь должен быть членом организации."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Успешно присоединился"),
        @ApiResponse(responseCode = "401", description = "Не авторизован"),
        @ApiResponse(responseCode = "403", description = "Не член организации"),
        @ApiResponse(responseCode = "404", description = "Группа не найдена по коду"),
        @ApiResponse(responseCode = "409", description = "Уже участник группы")
    })
    @PostMapping("/groups/join/{inviteCode}")
    public ResponseEntity<JoinGroupResponse> join(
        @AuthenticationPrincipal UserPrincipal principal,
        @PathVariable String inviteCode
    ) {
        requireAuthenticated(principal);
        JoinGroupResponse response = groupService.joinByInviteCode(principal.userId(), inviteCode);
        return ResponseEntity.ok(response);
    }

    // ==================== Вспомогательные методы ====================

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new AccessDeniedException("Требуется аутентификация");
        }
    }
}
