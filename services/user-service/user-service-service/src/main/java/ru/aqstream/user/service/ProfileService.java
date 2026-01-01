package ru.aqstream.user.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.user.api.dto.ChangePasswordRequest;
import ru.aqstream.user.api.dto.UpdateProfileRequest;
import ru.aqstream.user.api.dto.UserDto;
import ru.aqstream.user.api.exception.UserNotFoundException;
import ru.aqstream.user.api.exception.WrongPasswordException;
import ru.aqstream.user.db.entity.User;
import ru.aqstream.user.db.repository.UserRepository;

/**
 * Сервис для управления профилем пользователя.
 * Обновление личных данных и смена пароля.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final UserMapper userMapper;

    /**
     * Обновляет профиль пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param request данные для обновления
     * @return обновлённый профиль
     */
    @Transactional
    public UserDto updateProfile(UUID userId, UpdateProfileRequest request) {
        log.info("Обновление профиля: userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());

        user = userRepository.save(user);

        log.info("Профиль обновлён: userId={}", userId);

        return userMapper.toDto(user);
    }

    /**
     * Изменяет пароль пользователя.
     *
     * @param userId  идентификатор пользователя
     * @param request текущий и новый пароль
     */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.info("Смена пароля: userId={}", userId);

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // Проверяем текущий пароль
        if (!passwordService.matches(request.currentPassword(), user.getPasswordHash())) {
            log.debug("Неверный текущий пароль: userId={}", userId);
            throw new WrongPasswordException();
        }

        // Валидируем новый пароль
        passwordService.validate(request.newPassword());

        // Хешируем и сохраняем новый пароль
        String newPasswordHash = passwordService.hash(request.newPassword());
        user.setPasswordHash(newPasswordHash);
        userRepository.save(user);

        log.info("Пароль изменён: userId={}", userId);
    }
}
