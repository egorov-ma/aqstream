package ru.aqstream.user.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.api.dto.LinkTelegramByTokenRequest;
import ru.aqstream.user.api.exception.TelegramIdAlreadyExistsException;
import ru.aqstream.user.api.exception.TelegramLinkTokenNotFoundException;
import ru.aqstream.user.db.entity.TelegramLinkToken;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.TelegramLinkTokenRepository;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис для привязки Telegram аккаунта к существующему email-аккаунту.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramLinkService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_LENGTH = 32;

    private final TelegramLinkTokenRepository linkTokenRepository;
    private final UserRepository userRepository;

    /**
     * Создаёт токен для привязки Telegram к аккаунту пользователя.
     * Предыдущие неиспользованные токены инвалидируются.
     *
     * @param userId идентификатор пользователя
     * @return токен для привязки
     */
    @Transactional
    public String createLinkToken(UUID userId) {
        log.info("Создание токена привязки Telegram: userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден: " + userId));

        // Инвалидируем предыдущие неиспользованные токены
        int invalidated = linkTokenRepository.invalidateAllByUserId(userId, Instant.now());
        if (invalidated > 0) {
            log.debug("Инвалидировано предыдущих токенов: userId={}, count={}", userId, invalidated);
        }

        // Генерируем новый токен
        String token = generateToken();
        TelegramLinkToken linkToken = TelegramLinkToken.create(user, token);
        linkTokenRepository.save(linkToken);

        log.info("Токен привязки Telegram создан: userId={}", userId);
        return token;
    }

    /**
     * Привязывает Telegram к аккаунту по токену.
     * Вызывается ботом при обработке /start link_{token}.
     *
     * @param request данные для привязки
     */
    @Transactional
    public void linkTelegramByToken(LinkTelegramByTokenRequest request) {
        log.info("Привязка Telegram по токену: telegramId={}", request.telegramId());

        // Ищем токен
        TelegramLinkToken linkToken = linkTokenRepository.findByToken(request.linkToken())
            .orElseThrow(TelegramLinkTokenNotFoundException::new);

        // Проверяем валидность токена
        if (!linkToken.isValid()) {
            log.info("Токен привязки недействителен: expired={}, used={}",
                linkToken.isExpired(), linkToken.isUsed());
            throw new TelegramLinkTokenNotFoundException();
        }

        User user = linkToken.getUser();
        String telegramIdStr = String.valueOf(request.telegramId());

        // Проверяем, не привязан ли уже этот Telegram ID к другому аккаунту
        if (userRepository.existsByTelegramId(telegramIdStr)) {
            log.info("Telegram ID уже привязан к другому аккаунту: telegramId={}", request.telegramId());
            throw new TelegramIdAlreadyExistsException(telegramIdStr);
        }

        // Привязываем Telegram
        user.setTelegramId(telegramIdStr);
        user.setTelegramChatId(String.valueOf(request.telegramChatId()));
        userRepository.save(user);

        // Помечаем токен как использованный
        linkToken.markAsUsed();
        linkTokenRepository.save(linkToken);

        log.info("Telegram успешно привязан: userId={}, telegramId={}", user.getId(), request.telegramId());
    }

    /**
     * Генерирует криптографически безопасный токен.
     */
    private String generateToken() {
        byte[] bytes = new byte[TOKEN_LENGTH];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
