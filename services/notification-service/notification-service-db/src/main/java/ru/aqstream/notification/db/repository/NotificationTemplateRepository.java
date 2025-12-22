package ru.aqstream.notification.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.aqstream.notification.api.dto.NotificationChannel;
import ru.aqstream.notification.db.entity.NotificationTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для работы с шаблонами уведомлений.
 */
@Repository
public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    /**
     * Находит шаблон по коду и каналу.
     *
     * @param code    уникальный код шаблона
     * @param channel канал отправки
     * @return шаблон или empty
     */
    Optional<NotificationTemplate> findByCodeAndChannel(String code, NotificationChannel channel);

    /**
     * Находит шаблон по коду (для Telegram по умолчанию).
     *
     * @param code уникальный код шаблона
     * @return шаблон или empty
     */
    Optional<NotificationTemplate> findByCode(String code);

    /**
     * Проверяет существование шаблона по коду и каналу.
     *
     * @param code    уникальный код шаблона
     * @param channel канал отправки
     * @return true если шаблон существует
     */
    boolean existsByCodeAndChannel(String code, NotificationChannel channel);

    /**
     * Возвращает все шаблоны для указанного канала.
     *
     * @param channel канал отправки
     * @return список шаблонов
     */
    List<NotificationTemplate> findByChannel(NotificationChannel channel);

    /**
     * Возвращает все системные шаблоны.
     *
     * @return список системных шаблонов
     */
    List<NotificationTemplate> findByIsSystemTrue();
}
