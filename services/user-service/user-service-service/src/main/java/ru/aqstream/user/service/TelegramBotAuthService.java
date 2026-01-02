package ru.aqstream.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TokenHasher;
import ru.aqstream.common.security.UserPrincipal;
import static ru.aqstream.user.api.util.TelegramUtils.maskToken;

import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.ConfirmTelegramAuthRequest;
import ru.aqstream.user.api.dto.TelegramAuthInitResponse;
import ru.aqstream.user.api.dto.TelegramAuthStatusResponse;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.event.UserRegisteredEvent;
import ru.aqstream.user.api.exception.TelegramAuthTokenExpiredException;
import ru.aqstream.user.api.exception.TelegramAuthTokenNotFoundException;
import ru.aqstream.user.db.entity.RefreshToken;
import ru.aqstream.user.db.entity.TelegramAuthToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.TelegramAuthTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.websocket.TelegramAuthWebSocketHandler;

/**
 * Сервис авторизации через Telegram бота.
 *
 * <p>Реализует flow:
 * <ol>
 *   <li>initAuth() — создаёт токен и возвращает deeplink на бота</li>
 *   <li>confirmAuth() — подтверждает авторизацию (вызывается ботом)</li>
 *   <li>checkStatus() — возвращает статус токена для polling/fallback</li>
 * </ol>
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramBotAuthService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    /**
     * Максимальное количество активных сессий на пользователя.
     */
    public static final int MAX_ACTIVE_SESSIONS = 10;

    /**
     * Системный tenant для пользователей без привязки к организации.
     */
    private static final UUID SYSTEM_TENANT = new UUID(0L, 0L);

    private final TelegramAuthTokenRepository authTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;
    private final TelegramAuthWebSocketHandler webSocketHandler;

    @Value("${telegram.bot.username:AqStreamBot}")
    private String telegramBotUsername;

    @Value("${jwt.access-token-expiration:15m}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:7d}")
    private Duration refreshTokenExpiration;

    /**
     * Инициализирует авторизацию через Telegram бота.
     * Создаёт токен и возвращает deeplink для перехода в бота.
     *
     * @return данные для авторизации (токен, deeplink, время истечения)
     */
    @Transactional
    public TelegramAuthInitResponse initAuth() {
        log.info("Инициализация авторизации через Telegram бота");

        // Генерируем токен
        String token = generateToken();
        TelegramAuthToken authToken = TelegramAuthToken.create(token);
        authTokenRepository.save(authToken);

        // Формируем deeplink
        String deeplink = buildDeeplink(token);

        log.info("Токен авторизации создан: token={}", maskToken(token));

        return new TelegramAuthInitResponse(token, deeplink, authToken.getExpiresAt());
    }

    /**
     * Проверяет статус авторизации.
     * Используется для polling или как fallback если WebSocket не работает.
     *
     * @param token токен авторизации
     * @return статус и данные авторизации
     */
    @Transactional(readOnly = true)
    public TelegramAuthStatusResponse checkStatus(String token) {
        log.debug("Проверка статуса авторизации: token={}", maskToken(token));

        TelegramAuthToken authToken = authTokenRepository.findByToken(token)
            .orElseThrow(TelegramAuthTokenNotFoundException::new);

        // Проверяем истечение
        if (authToken.isExpired() && authToken.isPending()) {
            return TelegramAuthStatusResponse.expired();
        }

        // Возвращаем статус в зависимости от состояния токена
        return switch (authToken.getStatus()) {
            case PENDING -> TelegramAuthStatusResponse.pending();
            case EXPIRED -> TelegramAuthStatusResponse.expired();
            case CONFIRMED, USED -> {
                // Для CONFIRMED возвращаем данные, для USED — тоже (идемпотентность)
                if (authToken.getTelegramId() != null) {
                    // Находим пользователя
                    User user = userRepository.findByTelegramId(authToken.getTelegramId())
                        .orElse(null);
                    if (user != null) {
                        UserDto userDto = userMapper.toDto(user);
                        // Возвращаем только данные пользователя без токенов
                        // (токены выдаются через WebSocket или при первом запросе)
                        yield TelegramAuthStatusResponse.confirmed(null, 0, userDto);
                    }
                }
                yield TelegramAuthStatusResponse.expired();
            }
        };
    }

    /**
     * Подтверждает авторизацию.
     * Вызывается ботом при нажатии кнопки "Подтвердить вход".
     *
     * @param request данные от бота
     * @param userAgent User-Agent для сохранения сессии
     * @param ipAddress IP адрес для сохранения сессии
     */
    @Transactional
    public void confirmAuth(ConfirmTelegramAuthRequest request, String userAgent, String ipAddress) {
        String token = request.token();
        log.info("Подтверждение авторизации: token={}, telegramId={}",
            maskToken(token), request.telegramId());

        // Ищем pending токен
        TelegramAuthToken authToken = authTokenRepository.findPendingByToken(token, Instant.now())
            .orElseThrow(() -> {
                log.debug("Токен не найден или истёк: token={}", maskToken(token));
                return new TelegramAuthTokenExpiredException();
            });

        // Подтверждаем токен с данными пользователя
        authToken.confirm(
            request.telegramId().toString(),
            request.firstName(),
            request.lastName(),
            request.username(),
            request.chatId().toString()
        );
        authTokenRepository.save(authToken);

        // Создаём или находим пользователя и генерируем JWT
        AuthResponse authResponse = completeAuth(authToken, userAgent, ipAddress);

        // Помечаем токен как использованный
        authToken.markAsUsed();
        authTokenRepository.save(authToken);

        // Уведомляем frontend через WebSocket
        webSocketHandler.notifyConfirmation(token, authResponse);

        log.info("Авторизация подтверждена: token={}, userId={}",
            maskToken(token), authResponse.user().id());
    }

    /**
     * Завершает авторизацию — создаёт/находит пользователя и выдаёт JWT.
     */
    private AuthResponse completeAuth(TelegramAuthToken authToken, String userAgent, String ipAddress) {
        String telegramId = authToken.getTelegramId();

        // Ищем существующего пользователя
        User user = userRepository.findByTelegramId(telegramId).orElse(null);

        if (user != null) {
            // Вход существующего пользователя
            log.info("Вход через Telegram бота: userId={}", user.getId());
            user.recordSuccessfulLogin();

            // Обновляем chat_id если изменился
            if (!authToken.getTelegramChatId().equals(user.getTelegramChatId())) {
                user.setTelegramChatId(authToken.getTelegramChatId());
            }
            userRepository.save(user);
        } else {
            // Регистрация нового пользователя
            log.info("Регистрация через Telegram бота: telegramId={}",
                telegramId.substring(0, Math.min(4, telegramId.length())) + "***");

            user = User.createWithTelegram(
                telegramId,
                authToken.getTelegramChatId(),
                authToken.getTelegramFirstName(),
                authToken.getTelegramLastName(),
                null // photoUrl — нет в callback от бота
            );
            user.recordSuccessfulLogin();
            user = userRepository.save(user);

            log.info("Пользователь зарегистрирован через Telegram бота: userId={}", user.getId());

            // Публикуем событие для приветственного уведомления
            eventPublisher.publish(UserRegisteredEvent.forTelegram(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getTelegramChatId()
            ));
        }

        return createAuthResponse(user, userAgent, ipAddress);
    }

    /**
     * Создаёт AuthResponse с токенами.
     */
    private AuthResponse createAuthResponse(User user, String userAgent, String ipAddress) {
        // Создаём UserPrincipal для JWT
        Set<String> roles = user.isAdmin() ? Set.of("USER", "ADMIN") : Set.of("USER");
        UserPrincipal principal = new UserPrincipal(
            user.getId(),
            user.getEmail(),
            SYSTEM_TENANT,
            roles
        );

        // Генерируем токены
        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Сохраняем refresh token
        RefreshToken tokenEntity = RefreshToken.create(
            user,
            TokenHasher.hash(refreshToken),
            Instant.now().plus(refreshTokenExpiration),
            userAgent,
            ipAddress
        );
        refreshTokenRepository.save(tokenEntity);

        // Отзываем старые токены если превышен лимит сессий
        int revokedCount = refreshTokenRepository.revokeOldestTokensExceedingLimit(
            user.getId(), MAX_ACTIVE_SESSIONS, Instant.now()
        );
        if (revokedCount > 0) {
            log.debug("Отозвано старых сессий: userId={}, count={}", user.getId(), revokedCount);
        }

        // Формируем ответ
        UserDto userDto = userMapper.toDto(user);
        long expiresIn = accessTokenExpiration.toSeconds();

        return AuthResponse.bearer(accessToken, refreshToken, expiresIn, userDto);
    }

    /**
     * Генерирует криптографически безопасный токен.
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Формирует deeplink на бота.
     */
    private String buildDeeplink(String token) {
        // Удаляем @ из username если есть
        String botName = telegramBotUsername.startsWith("@")
            ? telegramBotUsername.substring(1)
            : telegramBotUsername;
        return String.format("https://t.me/%s?start=auth_%s", botName, token);
    }
}
