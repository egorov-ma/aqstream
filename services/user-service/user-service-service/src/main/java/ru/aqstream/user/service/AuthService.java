package ru.aqstream.user.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.security.JwtAuthenticationException;
import ru.aqstream.common.security.JwtTokenProvider;
import ru.aqstream.common.security.TokenHasher;
import ru.aqstream.common.security.UserPrincipal;
import ru.aqstream.user.api.dto.AuthResponse;
import ru.aqstream.user.api.dto.LoginRequest;
import ru.aqstream.user.api.dto.RefreshTokenRequest;
import ru.aqstream.user.api.dto.RegisterRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.exception.AccountLockedException;
import ru.aqstream.user.api.exception.EmailAlreadyExistsException;
import ru.aqstream.user.api.exception.InvalidCredentialsException;
import ru.aqstream.user.api.util.EmailUtils;
import ru.aqstream.user.db.entity.RefreshToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис аутентификации.
 * Регистрация, вход, обновление токенов, выход.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    /**
     * Максимальное количество активных сессий (refresh tokens) на пользователя.
     * При превышении старые сессии автоматически отзываются.
     */
    public static final int MAX_ACTIVE_SESSIONS = 10;

    /**
     * Системный tenant ID для пользователей без организации.
     * Используется при регистрации/входе до выбора организации.
     */
    private static final UUID SYSTEM_TENANT_ID = new UUID(0L, 0L);

    /**
     * Роли по умолчанию для новых пользователей.
     */
    private static final Set<String> DEFAULT_USER_ROLES = Set.of("USER");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserMapper userMapper;
    private final VerificationService verificationService;

    @Value("${jwt.access-token-expiration:15m}")
    private Duration accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration:7d}")
    private Duration refreshTokenExpiration;

    /**
     * Регистрирует нового пользователя по email.
     *
     * @param request   данные для регистрации
     * @param userAgent User-Agent клиента
     * @param ipAddress IP адрес клиента
     * @return токены и данные пользователя
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, String userAgent, String ipAddress) {
        log.info("Регистрация нового пользователя: email={}", EmailUtils.maskEmail(request.email()));

        // Проверяем уникальность email
        if (userRepository.existsByEmail(request.email())) {
            log.debug("Email уже зарегистрирован: {}", EmailUtils.maskEmail(request.email()));
            throw new EmailAlreadyExistsException();
        }

        // Валидируем пароль
        passwordService.validate(request.password());

        // Создаём пользователя
        String passwordHash = passwordService.hash(request.password());
        User user = User.createWithEmail(
            request.email(),
            passwordHash,
            request.firstName(),
            request.lastName()
        );
        user = userRepository.save(user);

        log.info("Пользователь зарегистрирован: userId={}", user.getId());

        // Создаём токен верификации email
        verificationService.createEmailVerificationToken(user);

        // Генерируем токены
        return createAuthResponse(user, userAgent, ipAddress);
    }

    /**
     * Выполняет вход пользователя по email и паролю.
     *
     * @param request   данные для входа
     * @param userAgent User-Agent клиента
     * @param ipAddress IP адрес клиента
     * @return токены и данные пользователя
     */
    @Transactional(noRollbackFor = {InvalidCredentialsException.class, AccountLockedException.class})
    public AuthResponse login(LoginRequest request, String userAgent, String ipAddress) {
        log.debug("Попытка входа: email={}", EmailUtils.maskEmail(request.email()));

        // Ищем пользователя
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> {
                log.debug("Пользователь не найден: email={}", EmailUtils.maskEmail(request.email()));
                return new InvalidCredentialsException();
            });

        // Проверяем блокировку аккаунта
        if (user.isLocked()) {
            log.debug("Аккаунт заблокирован: userId={}, lockedUntil={}",
                user.getId(), user.getLockedUntil());
            throw new AccountLockedException(user.getLockedUntil());
        }

        // Проверяем пароль
        if (!passwordService.matches(request.password(), user.getPasswordHash())) {
            user.recordFailedLogin();
            userRepository.save(user);

            log.debug("Неверный пароль: userId={}, failedAttempts={}",
                user.getId(), user.getFailedLoginAttempts());

            // Если аккаунт заблокировался после этой попытки
            if (user.isLocked()) {
                throw new AccountLockedException(user.getLockedUntil());
            }

            throw new InvalidCredentialsException();
        }

        // Успешный вход
        user.recordSuccessfulLogin();
        userRepository.save(user);

        log.info("Успешный вход: userId={}", user.getId());

        return createAuthResponse(user, userAgent, ipAddress);
    }

    /**
     * Обновляет токены по refresh token.
     *
     * @param request   refresh token
     * @param userAgent User-Agent клиента
     * @param ipAddress IP адрес клиента
     * @return новые токены
     */
    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String userAgent, String ipAddress) {
        // Валидируем JWT refresh token
        UUID userId;
        try {
            userId = jwtTokenProvider.validateRefreshToken(request.refreshToken());
        } catch (JwtAuthenticationException e) {
            log.debug("Невалидный JWT refresh token: {}", e.getMessage());
            throw new InvalidCredentialsException();
        }

        // Ищем токен в БД
        String tokenHash = TokenHasher.hash(request.refreshToken());
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow(() -> {
                log.debug("Refresh token не найден в БД");
                return new InvalidCredentialsException();
            });

        // Проверяем валидность
        if (!storedToken.isValid()) {
            log.debug("Refresh token невалиден: revoked={}, expired={}",
                storedToken.isRevoked(), storedToken.isExpired());
            throw new InvalidCredentialsException();
        }

        // Проверяем принадлежность пользователю
        if (!storedToken.getUser().getId().equals(userId)) {
            log.warn("Refresh token не принадлежит пользователю: tokenUserId={}, claimedUserId={}",
                storedToken.getUser().getId(), userId);
            throw new InvalidCredentialsException();
        }

        // Отзываем старый токен (one-time use)
        storedToken.revoke();
        refreshTokenRepository.save(storedToken);

        User user = storedToken.getUser();
        log.debug("Токены обновлены: userId={}", user.getId());

        return createAuthResponse(user, userAgent, ipAddress);
    }

    /**
     * Выполняет выход пользователя (отзывает все refresh токены).
     *
     * @param userId идентификатор пользователя
     */
    @Transactional
    public void logout(UUID userId) {
        int revokedCount = refreshTokenRepository.revokeAllByUserId(userId, Instant.now());
        log.info("Выход: userId={}, отозвано токенов={}", userId, revokedCount);
    }

    /**
     * Выполняет выход пользователя по refresh token (отзывает все его токены).
     * Валидирует токен, извлекает userId и отзывает все сессии пользователя.
     *
     * @param refreshToken refresh token пользователя
     * @throws InvalidCredentialsException если токен невалиден
     */
    @Transactional
    public void logoutAll(String refreshToken) {
        // Валидируем JWT и получаем userId
        UUID userId;
        try {
            userId = jwtTokenProvider.validateRefreshToken(refreshToken);
        } catch (JwtAuthenticationException e) {
            log.debug("Невалидный refresh token при logout: {}", e.getMessage());
            throw new InvalidCredentialsException();
        }

        // Отзываем все токены пользователя
        logout(userId);
    }

    /**
     * Отзывает конкретный refresh token.
     *
     * @param refreshToken токен для отзыва
     */
    @Transactional
    public void revokeToken(String refreshToken) {
        String tokenHash = TokenHasher.hash(refreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash)
            .ifPresent(token -> {
                token.revoke();
                refreshTokenRepository.save(token);
                log.debug("Refresh token отозван");
            });
    }

    // === Приватные методы ===

    /**
     * Создаёт AuthResponse с новыми токенами.
     */
    private AuthResponse createAuthResponse(User user, String userAgent, String ipAddress) {
        // Создаём UserPrincipal для JWT
        // При регистрации/входе пользователь пока не связан с организацией,
        // используем системный tenant (будет изменён после выбора организации)
        Set<String> roles = user.isAdmin()
            ? Set.of("USER", "ADMIN")
            : DEFAULT_USER_ROLES;

        UserPrincipal principal = new UserPrincipal(
            user.getId(),
            user.getEmail(),
            SYSTEM_TENANT_ID,
            roles
        );

        // Генерируем access token
        String accessToken = jwtTokenProvider.generateAccessToken(principal);

        // Генерируем refresh token
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // Сохраняем refresh token в БД (хешированный)
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
