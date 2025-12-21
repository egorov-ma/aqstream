package ru.aqstream.user.service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.EventPublisher;
import ru.aqstream.user.api.event.EmailVerificationRequestedEvent;
import ru.aqstream.user.api.event.PasswordResetRequestedEvent;
import ru.aqstream.user.api.exception.EmailAlreadyVerifiedException;
import ru.aqstream.user.api.exception.InvalidVerificationTokenException;
import ru.aqstream.user.api.exception.TooManyVerificationRequestsException;
import ru.aqstream.user.api.util.EmailUtils;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.entity.VerificationToken;
import ru.aqstream.user.db.entity.VerificationToken.TokenType;
import ru.aqstream.user.db.repository.RefreshTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;
import ru.aqstream.user.db.repository.VerificationTokenRepository;

/**
 * Сервис верификации email и сброса пароля.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    /**
     * Максимальное количество запросов на верификацию/сброс за период.
     */
    public static final int MAX_REQUESTS_PER_HOUR = 3;

    /**
     * Период для rate limiting.
     */
    public static final Duration RATE_LIMIT_PERIOD = Duration.ofHours(1);

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final EventPublisher eventPublisher;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    // === Email Verification ===

    /**
     * Создаёт токен верификации email и отправляет письмо.
     * Вызывается при регистрации по email.
     *
     * @param user пользователь
     * @return созданный токен (для логирования в dev mode)
     */
    @Transactional
    public String createEmailVerificationToken(User user) {
        // Инвалидируем старые токены
        verificationTokenRepository.invalidateAllByUserIdAndType(
            user.getId(),
            TokenType.EMAIL_VERIFICATION,
            Instant.now()
        );

        // Создаём новый токен
        String token = generateToken();
        VerificationToken verificationToken = VerificationToken.createEmailVerification(user, token);
        verificationTokenRepository.save(verificationToken);

        // Отправляем письмо
        sendVerificationEmail(user, token);

        log.info("Токен верификации email создан: userId={}", user.getId());

        return token;
    }

    /**
     * Подтверждает email по токену.
     *
     * @param token токен из письма
     */
    @Transactional
    public void verifyEmail(String token) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
            .orElseThrow(() -> {
                log.debug("Токен верификации не найден");
                return new InvalidVerificationTokenException();
            });

        // Проверяем валидность
        if (!verificationToken.isValid()) {
            log.debug("Токен верификации невалиден: used={}, expired={}",
                verificationToken.isUsed(), verificationToken.isExpired());
            throw new InvalidVerificationTokenException();
        }

        // Проверяем тип токена
        if (verificationToken.getType() != TokenType.EMAIL_VERIFICATION) {
            log.debug("Неверный тип токена: expected=EMAIL_VERIFICATION, actual={}",
                verificationToken.getType());
            throw new InvalidVerificationTokenException();
        }

        // Помечаем токен как использованный
        verificationToken.markAsUsed();
        verificationTokenRepository.save(verificationToken);

        // Подтверждаем email пользователя
        User user = verificationToken.getUser();
        user.verifyEmail();
        userRepository.save(user);

        log.info("Email подтверждён: userId={}", user.getId());
    }

    /**
     * Повторно отправляет письмо верификации.
     *
     * @param email email пользователя
     */
    @Transactional
    public void resendVerificationEmail(String email) {
        // Ищем пользователя (не раскрываем существование аккаунта)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.debug("Пользователь не найден для resend: email={}", EmailUtils.maskEmail(email));
            // Возвращаем успех чтобы не раскрывать существование email
            return;
        }

        // Проверяем что email ещё не подтверждён
        if (user.isEmailVerified()) {
            log.debug("Email уже подтверждён: userId={}", user.getId());
            throw new EmailAlreadyVerifiedException();
        }

        // Проверяем rate limit
        checkRateLimit(user.getId(), TokenType.EMAIL_VERIFICATION);

        // Создаём и отправляем токен
        createEmailVerificationToken(user);

        log.info("Письмо верификации отправлено повторно: userId={}", user.getId());
    }

    // === Password Reset ===

    /**
     * Запрашивает сброс пароля.
     * Создаёт токен и отправляет письмо со ссылкой.
     *
     * @param email email пользователя
     */
    @Transactional
    public void requestPasswordReset(String email) {
        // Ищем пользователя (не раскрываем существование аккаунта)
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            log.debug("Пользователь не найден для сброса пароля: email={}", EmailUtils.maskEmail(email));
            // Возвращаем успех чтобы не раскрывать существование email
            return;
        }

        // Проверяем rate limit
        checkRateLimit(user.getId(), TokenType.PASSWORD_RESET);

        // Инвалидируем старые токены сброса
        verificationTokenRepository.invalidateAllByUserIdAndType(
            user.getId(),
            TokenType.PASSWORD_RESET,
            Instant.now()
        );

        // Создаём новый токен
        String token = generateToken();
        VerificationToken verificationToken = VerificationToken.createPasswordReset(user, token);
        verificationTokenRepository.save(verificationToken);

        // Отправляем письмо
        sendPasswordResetEmail(user, token);

        log.info("Запрос сброса пароля: userId={}", user.getId());
    }

    /**
     * Сбрасывает пароль по токену.
     *
     * @param token       токен из письма
     * @param newPassword новый пароль
     */
    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken verificationToken = verificationTokenRepository.findByToken(token)
            .orElseThrow(() -> {
                log.debug("Токен сброса пароля не найден");
                return new InvalidVerificationTokenException();
            });

        // Проверяем валидность
        if (!verificationToken.isValid()) {
            log.debug("Токен сброса пароля невалиден: used={}, expired={}",
                verificationToken.isUsed(), verificationToken.isExpired());
            throw new InvalidVerificationTokenException();
        }

        // Проверяем тип токена
        if (verificationToken.getType() != TokenType.PASSWORD_RESET) {
            log.debug("Неверный тип токена: expected=PASSWORD_RESET, actual={}",
                verificationToken.getType());
            throw new InvalidVerificationTokenException();
        }

        // Валидируем новый пароль
        passwordService.validate(newPassword);

        // Помечаем токен как использованный
        verificationToken.markAsUsed();
        verificationTokenRepository.save(verificationToken);

        // Обновляем пароль
        User user = verificationToken.getUser();
        user.setPasswordHash(passwordService.hash(newPassword));
        userRepository.save(user);

        // Отзываем все refresh токены (завершаем все сессии)
        int revokedCount = refreshTokenRepository.revokeAllByUserId(user.getId(), Instant.now());

        log.info("Пароль сброшен: userId={}, отозвано сессий={}", user.getId(), revokedCount);
    }

    // === Cleanup ===

    /**
     * Удаляет истёкшие и использованные токены.
     * Вызывается по расписанию.
     *
     * @return количество удалённых токенов
     */
    @Transactional
    public int cleanupExpiredTokens() {
        Instant now = Instant.now();
        Instant usedBefore = now.minus(Duration.ofDays(7)); // Храним использованные 7 дней для аудита

        int expiredDeleted = verificationTokenRepository.deleteExpiredBefore(now);
        int usedDeleted = verificationTokenRepository.deleteUsedBefore(usedBefore);

        int total = expiredDeleted + usedDeleted;
        if (total > 0) {
            log.info("Очистка токенов верификации: удалено истёкших={}, использованных={}",
                expiredDeleted, usedDeleted);
        }

        return total;
    }

    // === Приватные методы ===

    /**
     * Проверяет rate limit на запросы.
     */
    private void checkRateLimit(UUID userId, TokenType type) {
        Instant since = Instant.now().minus(RATE_LIMIT_PERIOD);
        long requestCount = verificationTokenRepository.countByUserIdAndTypeSince(userId, type, since);

        if (requestCount >= MAX_REQUESTS_PER_HOUR) {
            long retryAfterMinutes = RATE_LIMIT_PERIOD.toMinutes();
            log.debug("Rate limit превышен: userId={}, type={}, count={}",
                userId, type, requestCount);
            throw new TooManyVerificationRequestsException(retryAfterMinutes);
        }
    }

    /**
     * Генерирует уникальный токен.
     */
    private String generateToken() {
        // Используем UUID без дефисов (32 символа hex)
        return UUID.randomUUID().toString().replace("-", "")
            + UUID.randomUUID().toString().replace("-", "").substring(0, 32);
    }

    /**
     * Отправляет письмо верификации email через notification-service.
     */
    private void sendVerificationEmail(User user, String token) {
        String verificationLink = frontendUrl + "/verify-email?token=" + token;

        // В dev mode выводим ссылку в лог (email маскируется для безопасности)
        log.info("=== EMAIL VERIFICATION ===");
        log.info("To: {}", EmailUtils.maskEmail(user.getEmail()));
        log.info("Subject: Подтверждение email на AqStream");
        log.info("Verification link: {}", verificationLink);
        log.info("Token expires in: {} hours", VerificationToken.EMAIL_VERIFICATION_EXPIRATION_HOURS);
        log.info("==========================");

        // Публикуем событие для notification-service
        eventPublisher.publish(new EmailVerificationRequestedEvent(
            user.getId(),
            user.getEmail(),
            token,
            verificationLink
        ));
    }

    /**
     * Отправляет письмо для сброса пароля через notification-service.
     */
    private void sendPasswordResetEmail(User user, String token) {
        String resetLink = frontendUrl + "/reset-password?token=" + token;

        // В dev mode выводим ссылку в лог (email маскируется для безопасности)
        log.info("=== PASSWORD RESET ===");
        log.info("To: {}", EmailUtils.maskEmail(user.getEmail()));
        log.info("Subject: Сброс пароля на AqStream");
        log.info("Reset link: {}", resetLink);
        log.info("Token expires in: {} hour", VerificationToken.PASSWORD_RESET_EXPIRATION_HOURS);
        log.info("======================");

        // Публикуем событие для notification-service
        eventPublisher.publish(new PasswordResetRequestedEvent(
            user.getId(),
            user.getEmail(),
            token,
            resetLink
        ));
    }
}
