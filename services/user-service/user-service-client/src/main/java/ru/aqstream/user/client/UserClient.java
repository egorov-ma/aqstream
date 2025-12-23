package ru.aqstream.user.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.aqstream.user.api.dto.AcceptInviteByTelegramRequest;
import ru.aqstream.user.api.dto.LinkTelegramByTokenRequest;
import ru.aqstream.user.api.dto.OrganizationMemberDto;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.dto.UserTelegramInfoDto;

import java.util.Optional;
import java.util.UUID;

/**
 * Feign клиент для User Service.
 * Используется другими сервисами для получения данных пользователей.
 */
@FeignClient(
        name = "user-service",
        url = "${user-service.url:http://localhost:8081}"
)
public interface UserClient {

    /**
     * Получает публичные данные пользователя по ID.
     *
     * @param userId ID пользователя
     * @return данные пользователя или пустой Optional
     */
    @GetMapping("/api/v1/internal/users/{userId}")
    Optional<UserDto> findById(@PathVariable("userId") UUID userId);

    /**
     * Получает Telegram информацию пользователя для отправки уведомлений.
     *
     * @param userId ID пользователя
     * @return Telegram информация или пустой Optional
     */
    @GetMapping("/api/v1/internal/users/{userId}/telegram")
    Optional<UserTelegramInfoDto> findTelegramInfo(@PathVariable("userId") UUID userId);

    /**
     * Очищает telegram_chat_id пользователя (когда пользователь заблокировал бота).
     *
     * @param chatId Telegram Chat ID
     */
    @DeleteMapping("/api/v1/internal/users/telegram-chat")
    void clearTelegramChatId(@RequestParam("chatId") String chatId);

    /**
     * Проверяет членство пользователя в группе.
     * Используется для проверки доступа к приватным событиям.
     *
     * @param groupId ID группы
     * @param userId  ID пользователя
     * @return true если пользователь член группы
     */
    @GetMapping("/api/v1/internal/users/groups/{groupId}/members/{userId}/exists")
    Boolean isGroupMember(@PathVariable("groupId") UUID groupId, @PathVariable("userId") UUID userId);

    /**
     * Ищет пользователя по Telegram ID.
     * Используется ботом для идентификации пользователя при обработке deeplinks.
     *
     * @param telegramId Telegram ID
     * @return данные пользователя или пустой Optional
     */
    @GetMapping("/api/v1/internal/users/telegram/{telegramId}")
    Optional<UserDto> findByTelegramId(@PathVariable("telegramId") String telegramId);

    /**
     * Принимает приглашение в организацию через Telegram deeplink.
     *
     * @param request данные для принятия приглашения
     * @return членство в организации
     */
    @PostMapping("/api/v1/internal/users/organizations/accept-invite")
    OrganizationMemberDto acceptInviteByTelegram(@RequestBody AcceptInviteByTelegramRequest request);

    /**
     * Привязывает Telegram к аккаунту по токену.
     * Вызывается ботом при обработке /start link_{token}.
     *
     * @param request данные для привязки
     */
    @PostMapping("/api/v1/internal/users/link-telegram")
    void linkTelegramByToken(@RequestBody LinkTelegramByTokenRequest request);
}
