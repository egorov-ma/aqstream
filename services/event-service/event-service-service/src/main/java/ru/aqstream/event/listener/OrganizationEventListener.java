package ru.aqstream.event.listener;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.aqstream.common.messaging.config.RabbitMQConfig;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.db.entity.Event;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.user.api.event.OrganizationDeletedEvent;

/**
 * Слушатель событий организаций из RabbitMQ.
 *
 * <p>Обрабатывает события из очереди event-service.queue, связанные
 * с жизненным циклом организаций.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrganizationEventListener {

    private final EventRepository eventRepository;

    /**
     * Обрабатывает удаление организации.
     * Архивирует (soft delete) все активные события организации.
     *
     * @param event событие удаления организации
     */
    @RabbitListener(queues = RabbitMQConfig.EVENT_SERVICE_QUEUE, id = "organization-deleted")
    @Transactional
    public void handleOrganizationDeleted(OrganizationDeletedEvent event) {
        log.info("Получено событие OrganizationDeletedEvent: organizationId={}, name={}",
            event.getOrganizationId(), event.getOrganizationName());

        try {
            // Устанавливаем TenantContext для корректной работы RLS
            TenantContext.setTenantId(event.getOrganizationId());

            // Находим все активные события организации
            List<Event> activeEvents = eventRepository.findActiveByTenantId(event.getOrganizationId());

            if (activeEvents.isEmpty()) {
                log.info("Нет активных событий для архивирования: organizationId={}",
                    event.getOrganizationId());
                return;
            }

            log.info("Архивирование событий при удалении организации: organizationId={}, count={}",
                event.getOrganizationId(), activeEvents.size());

            // Архивируем все события (soft delete)
            for (Event e : activeEvents) {
                e.softDelete();
            }
            eventRepository.saveAll(activeEvents);

            log.info("События архивированы: organizationId={}, count={}",
                event.getOrganizationId(), activeEvents.size());

        } catch (Exception e) {
            log.error("Ошибка обработки OrganizationDeletedEvent: organizationId={}, error={}",
                event.getOrganizationId(), e.getMessage(), e);
            throw e; // Пробрасываем для DLQ
        } finally {
            TenantContext.clear();
        }
    }
}
