package ru.aqstream.user.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TokenHasher;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.TelegramAuthRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.event.UserRegisteredEvent;
import ru.aqstream.user.api.exception.InvalidTelegramAuthException;
import ru.aqstream.user.api.exception.TelegramIdAlreadyExistsException;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.api.util.TelegramUtils;
import ru.aqstream.user.db.entity.RefreshToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис аутентификации через Telegram.
 *
 * <p>Реализует вход и регистрацию через Telegram Login Widget
 * согласно https://core.telegram.org/widgets/login</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramAuthService {

    /**
     * Максимальный возраст auth_date в секундах (1 час).
     */
    private static final long MAX_AUTH_AGE_SECONDS = 3600;

    /**
     * Максимальное количество активных сессий на пользователя.
     */
    public static final int MAX_ACTIVE_SESSIONS = 10;

    /**
     * Системный tenant для пользователей без привязки к организации.
     * Используется для Telegram-only пользователей и новых регистраций.
     */
    private static final UUID SYSTEM_TENANT = new UUID(0L, 0L);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final EventPublisher eventPublisher;

    @Value("${telegram.bot.token:}")
    private String telegramBotToken;

    @Value("${jwt.access-token-expiration:15m}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:7d}")
    private Duration refreshTokenExpiration;

    /**
     * Выполняет аутентификацию через Telegram.
     *
     * <p>Если пользователь с таким Telegram ID существует — выполняет вход.
     * Если не существует — создаёт нового пользователя.</p>
     *
     * @param request   данные от Telegram Login Widget
     * @param userAgent User-Agent клиента
     * @param ipAddress IP адрес клиента
     * @return токены и данные пользователя
     */
    @Transactional
    public AuthResponse authenticate(TelegramAuthRequest request, String userAgent, String ipAddress) {
        String telegramId = request.id().toString();
        log.info("Telegram аутентификация: telegramId={}", TelegramUtils.maskTelegramId(telegramId));

        // Валидируем данные от Telegram
        validateTelegramAuth(request);

        // Ищем существующего пользователя
        User user = userRepository.findByTelegramId(telegramId).orElse(null);

        if (user != null) {
            // Вход существующего пользователя
            log.info("Вход через Telegram: userId={}", user.getId());
            user.recordSuccessfulLogin();

            // Обновляем данные профиля из Telegram если изменились
            updateUserFromTelegram(user, request);
            userRepository.save(user);
        } else {
            // Регистрация нового пользователя
            log.info("Регистрация через Telegram: telegramId={}", TelegramUtils.maskTelegramId(telegramId));

            user = User.createWithTelegram(
                telegramId,
                telegramId, // chat_id совпадает с user_id для личных сообщений
                request.firstName(),
                request.lastName(),
                request.photoUrl()
            );
            user.recordSuccessfulLogin();
            user = userRepository.save(user);

            log.info("Пользователь зарегистрирован через Telegram: userId={}", user.getId());

            // Публикуем событие для отправки приветственного уведомления
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
     * Привязывает Telegram к существующему аккаунту пользователя.
     *
     * @param userId    ID пользователя
     * @param request   данные от Telegram Login Widget
     * @param userAgent User-Agent клиента
     * @param ipAddress IP адрес клиента
     * @return обновлённые токены и данные пользователя
     */
    @Transactional
    public AuthResponse linkTelegram(
        UUID userId,
        TelegramAuthRequest request,
        String userAgent,
        String ipAddress
    ) {
        String telegramId = request.id().toString();
        log.info("Привязка Telegram к аккаунту: userId={}, telegramId={}",
            userId, TelegramUtils.maskTelegramId(telegramId));

        // Валидируем данные от Telegram
        validateTelegramAuth(request);

        // Проверяем, не привязан ли уже этот Telegram к другому аккаунту
        if (userRepository.existsByTelegramId(telegramId)) {
            log.debug("Telegram уже привязан к другому аккаунту: telegramId={}",
                TelegramUtils.maskTelegramId(telegramId));
            throw new TelegramIdAlreadyExistsException();
        }

        // Находим пользователя
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Привязываем Telegram
        user.setTelegramId(telegramId);
        user.setTelegramChatId(telegramId);

        // Обновляем фото если его нет
        if (user.getAvatarUrl() == null && request.photoUrl() != null) {
            user.setAvatarUrl(request.photoUrl());
        }

        userRepository.save(user);
        log.info("Telegram привязан к аккаунту: userId={}", userId);

        return createAuthResponse(user, userAgent, ipAddress);
    }

    /**
     * Валидирует данные от Telegram Login Widget.
     *
     * @param request данные от Telegram
     * @throws InvalidTelegramAuthException если валидация не пройдена
     */
    private void validateTelegramAuth(TelegramAuthRequest request) {
        // Проверяем, что bot token сконфигурирован
        if (telegramBotToken == null || telegramBotToken.isBlank()) {
            log.error("Telegram Bot Token не настроен");
            throw new InvalidTelegramAuthException("сервис временно недоступен");
        }

        // Проверяем возраст auth_date
        long authAge = Instant.now().getEpochSecond() - request.authDate();
        if (authAge > MAX_AUTH_AGE_SECONDS) {
            log.debug("Telegram auth_date слишком старый: authAge={}с", authAge);
            throw new InvalidTelegramAuthException("данные устарели");
        }

        if (authAge < 0) {
            log.debug("Telegram auth_date в будущем: authAge={}с", authAge);
            throw new InvalidTelegramAuthException("некорректное время авторизации");
        }

        // Проверяем hash
        String calculatedHash = calculateTelegramHash(request);
        if (!calculatedHash.equalsIgnoreCase(request.hash())) {
            log.debug("Telegram hash не совпадает");
            throw new InvalidTelegramAuthException();
        }
    }

    /**
     * Вычисляет hash для валидации данных от Telegram.
     *
     * <p>Алгоритм согласно https://core.telegram.org/widgets/login#checking-authorization</p>
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity") // Алгоритм Telegram требует условных проверок
    private String calculateTelegramHash(TelegramAuthRequest request) {
        try {
            // Собираем data-check-string в алфавитном порядке
            TreeMap<String, String> data = new TreeMap<>();
            data.put("id", request.id().toString());
            data.put("first_name", request.firstName());
            if (request.lastName() != null && !request.lastName().isBlank()) {
                data.put("last_name", request.lastName());
            }
            if (request.username() != null && !request.username().isBlank()) {
                data.put("username", request.username());
            }
            if (request.photoUrl() != null && !request.photoUrl().isBlank()) {
                data.put("photo_url", request.photoUrl());
            }
            data.put("auth_date", request.authDate().toString());

            StringBuilder dataCheckString = new StringBuilder();
            for (var entry : data.entrySet()) {
                if (dataCheckString.length() > 0) {
                    dataCheckString.append("\n");
                }
                dataCheckString.append(entry.getKey()).append("=").append(entry.getValue());
            }

            // Secret key = SHA256(bot_token)
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] secretKey = sha256.digest(telegramBotToken.getBytes(StandardCharsets.UTF_8));

            // Hash = HMAC-SHA256(data_check_string, secret_key)
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(secretKey, "HmacSHA256"));
            byte[] hashBytes = hmac.doFinal(dataCheckString.toString().getBytes(StandardCharsets.UTF_8));

            // Конвертируем в hex
            StringBuilder hexHash = new StringBuilder();
            for (byte b : hashBytes) {
                hexHash.append(String.format("%02x", b));
            }

            return hexHash.toString();

        } catch (Exception e) {
            log.error("Ошибка вычисления Telegram hash", e);
            throw new InvalidTelegramAuthException("ошибка проверки подписи");
        }
    }

    /**
     * Обновляет данные пользователя из Telegram если они изменились.
     */
    private void updateUserFromTelegram(User user, TelegramAuthRequest request) {
        boolean updated = false;

        // Обновляем chat_id (на случай если изменился)
        if (user.getTelegramChatId() == null ||
            !user.getTelegramChatId().equals(request.id().toString())) {
            user.setTelegramChatId(request.id().toString());
            updated = true;
        }

        // Обновляем фото если изменилось
        if (request.photoUrl() != null && !request.photoUrl().equals(user.getAvatarUrl())) {
            user.setAvatarUrl(request.photoUrl());
            updated = true;
        }

        if (updated) {
            log.debug("Обновлены данные пользователя из Telegram: userId={}", user.getId());
        }
    }

    /**
     * Создаёт AuthResponse с токенами.
     */
    private AuthResponse createAuthResponse(User user, String userAgent, String ipAddress) {
        // Создаём UserPrincipal для JWT
        UserPrincipal principal = new UserPrincipal(
            user.getId(),
            user.getEmail(), // может быть null для Telegram-only пользователей
            SYSTEM_TENANT,
            Set.of("USER")
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
}
