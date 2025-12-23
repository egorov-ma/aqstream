package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.user.api.dto.AcceptInviteByTelegramRequest;
import ru.aqstream.user.api.dto.LinkTelegramByTokenRequest;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.dto.UserTelegramInfoDto;
import ru.aqstream.user.db.repository.GroupMemberRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.service.OrganizationService;
import ru.aqstream.user.service.TelegramLinkService;
import ru.aqstream.user.service.UserMapper;

/**
 * Внутренний контроллер для межсервисного взаимодействия.
 * Используется другими сервисами через Feign клиенты.
 *
 * <p>Эндпоинты не требуют аутентификации пользователя,
 * но должны быть защищены на уровне сети (внутренняя сеть).</p>
 */
@RestController
@RequestMapping("/api/v1/internal/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Users", description = "Внутренние эндпоинты для межсервисного взаимодействия")
public class InternalUserController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final GroupMemberRepository groupMemberRepository;
    private final OrganizationService organizationService;
    private final TelegramLinkService telegramLinkService;

    /**
     * Получает данные пользователя по ID.
     *
     * @param userId ID пользователя
     * @return данные пользователя или 404
     */
    @Operation(summary = "Получить пользователя по ID", description = "Внутренний эндпоинт для других сервисов")
    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> findById(@PathVariable UUID userId) {
        log.debug("Internal: запрос пользователя по ID: userId={}", userId);

        return userRepository.findById(userId)
            .map(userMapper::toDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Получает Telegram информацию пользователя для отправки уведомлений.
     *
     * @param userId ID пользователя
     * @return Telegram информация или 404
     */
    @Operation(
        summary = "Получить Telegram информацию",
        description = "Возвращает chat_id и имя для отправки уведомлений"
    )
    @GetMapping("/{userId}/telegram")
    public ResponseEntity<UserTelegramInfoDto> findTelegramInfo(@PathVariable UUID userId) {
        log.debug("Internal: запрос Telegram информации: userId={}", userId);

        return userRepository.findById(userId)
            .map(userMapper::toTelegramInfoDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Очищает telegram_chat_id пользователя (когда пользователь заблокировал бота).
     *
     * @param chatId Telegram Chat ID
     */
    @Operation(
        summary = "Очистить Telegram chat_id",
        description = "Вызывается когда пользователь заблокировал бота"
    )
    @DeleteMapping("/telegram-chat")
    public ResponseEntity<Void> clearTelegramChatId(@RequestParam String chatId) {
        log.info("Internal: очистка Telegram chat_id: chatId=***{}",
            chatId.length() > 3 ? chatId.substring(chatId.length() - 3) : "");

        userRepository.findByTelegramId(chatId)
            .or(() -> userRepository.findAll().stream()
                .filter(u -> chatId.equals(u.getTelegramChatId()))
                .findFirst())
            .ifPresent(user -> {
                user.setTelegramChatId(null);
                userRepository.save(user);
                log.info("Telegram chat_id очищен: userId={}", user.getId());
            });

        return ResponseEntity.ok().build();
    }

    /**
     * Проверяет членство пользователя в группе.
     * Используется для проверки доступа к приватным событиям.
     *
     * @param groupId ID группы
     * @param userId  ID пользователя
     * @return true если пользователь член группы
     */
    @Operation(
        summary = "Проверить членство в группе",
        description = "Проверяет является ли пользователь членом указанной группы"
    )
    @GetMapping("/groups/{groupId}/members/{userId}/exists")
    public ResponseEntity<Boolean> isGroupMember(
        @PathVariable UUID groupId,
        @PathVariable UUID userId
    ) {
        log.debug("Internal: проверка членства в группе: groupId={}, userId={}", groupId, userId);

        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        return ResponseEntity.ok(isMember);
    }

    /**
     * Ищет пользователя по Telegram ID.
     * Используется ботом для идентификации пользователя при обработке deeplinks.
     *
     * @param telegramId Telegram ID
     * @return данные пользователя или 404
     */
    @Operation(
        summary = "Найти пользователя по Telegram ID",
        description = "Возвращает пользователя по его Telegram ID"
    )
    @GetMapping("/telegram/{telegramId}")
    public ResponseEntity<UserDto> findByTelegramId(@PathVariable String telegramId) {
        log.debug("Internal: поиск пользователя по Telegram ID: telegramId=***{}",
            telegramId.length() > 3 ? telegramId.substring(telegramId.length() - 3) : "");

        return userRepository.findByTelegramId(telegramId)
            .map(userMapper::toDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Принимает приглашение в организацию через Telegram deeplink.
     * Вызывается ботом при обработке /start invite_{code}.
     *
     * @param request данные для принятия приглашения
     * @return членство в организации
     */
    @Operation(
        summary = "Принять приглашение в организацию",
        description = "Вызывается ботом при обработке deeplink /start invite_{code}"
    )
    @PostMapping("/organizations/accept-invite")
    public ResponseEntity<OrganizationMemberDto> acceptInviteByTelegram(
        @Valid @RequestBody AcceptInviteByTelegramRequest request
    ) {
        log.info("Internal: принятие приглашения через Telegram: userId={}", request.userId());

        OrganizationMemberDto member = organizationService.acceptInvite(
            request.userId(),
            request.inviteCode()
        );

        return ResponseEntity.ok(member);
    }

    /**
     * Привязывает Telegram к аккаунту по токену.
     * Вызывается ботом при обработке /start link_{token}.
     *
     * @param request данные для привязки
     * @return 200 OK при успешной привязке
     */
    @Operation(
        summary = "Привязать Telegram к аккаунту",
        description = "Вызывается ботом при обработке deeplink /start link_{token}"
    )
    @PostMapping("/link-telegram")
    public ResponseEntity<Void> linkTelegramByToken(
        @Valid @RequestBody LinkTelegramByTokenRequest request
    ) {
        log.info("Internal: привязка Telegram по токену: telegramId={}", request.telegramId());

        telegramLinkService.linkTelegramByToken(request);

        return ResponseEntity.ok().build();
    }
}
