package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.web.ClientIpResolver;
import ru.aqstream.user.api.dto.AcceptInviteByTelegramRequest;
import ru.aqstream.user.api.dto.ConfirmTelegramAuthRequest;
import ru.aqstream.user.api.dto.LinkTelegramByTokenRequest;
import ru.aqstream.user.api.dto.OrganizationDto;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.dto.UserTelegramInfoDto;
import ru.aqstream.user.db.repository.GroupMemberRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.service.OrganizationInviteService;
import ru.aqstream.user.service.OrganizationService;
import ru.aqstream.user.service.TelegramBotAuthService;
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
    private final OrganizationInviteService inviteService;
    private final TelegramLinkService telegramLinkService;
    private final TelegramBotAuthService telegramBotAuthService;

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

        // Ищем по telegram_id или по telegram_chat_id
        userRepository.findByTelegramId(chatId)
            .or(() -> userRepository.findByTelegramChatId(chatId))
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

        OrganizationMemberDto member = inviteService.acceptInvite(
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

    /**
     * Получает организацию по ID.
     * Используется event-service для получения названия организатора.
     *
     * @param organizationId ID организации
     * @return данные организации или 404
     */
    @Operation(
        summary = "Получить организацию по ID",
        description = "Внутренний эндпоинт для получения данных организации"
    )
    @GetMapping("/organizations/{organizationId}")
    public ResponseEntity<OrganizationDto> findOrganizationById(@PathVariable UUID organizationId) {
        log.debug("Internal: запрос организации по ID: organizationId={}", organizationId);

        return organizationService.findByIdInternal(organizationId)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Подтверждает авторизацию через Telegram бота.
     * Вызывается ботом при нажатии кнопки "Подтвердить вход".
     *
     * @param request     данные для подтверждения
     * @param httpRequest HTTP запрос для извлечения IP
     * @return 200 OK при успешном подтверждении
     */
    @Operation(
        summary = "Подтвердить авторизацию через Telegram",
        description = "Вызывается ботом при нажатии кнопки подтверждения входа"
    )
    @PostMapping("/auth/telegram/confirm")
    public ResponseEntity<Void> confirmTelegramAuth(
        @Valid @RequestBody ConfirmTelegramAuthRequest request,
        HttpServletRequest httpRequest
    ) {
        log.info("Internal: подтверждение авторизации через Telegram: telegramId={}", request.telegramId());

        String userAgent = httpRequest.getHeader("User-Agent");
        String ipAddress = ClientIpResolver.resolve(httpRequest);

        telegramBotAuthService.confirmAuth(request, userAgent, ipAddress);

        return ResponseEntity.ok().build();
    }
}
