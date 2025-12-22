package ru.aqstream.notification.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aqstream.notification.db.entity.NotificationPreference;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с настройками уведомлений.
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {

    /**
     * Находит настройки пользователя.
     *
     * @param userId ID пользователя
     * @return настройки или empty
     */
    Optional<NotificationPreference> findByUserId(UUID userId);

    /**
     * Проверяет существование настроек пользователя.
     *
     * @param userId ID пользователя
     * @return true если настройки существуют
     */
    boolean existsByUserId(UUID userId);
}
