package ru.aqstream.notification.scheduler;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.EventStatus;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.api.dto.RegistrationStatus;
import ru.aqstream.event.client.EventClient;
import ru.aqstream.notification.config.NotificationProperties;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.service.NotificationService;

/**
 * Планировщик отправки напоминаний о событиях.
 *
 * <p>Отправляет напоминания участникам за 24 часа до начала события.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EventReminderScheduler {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("d MMMM yyyy, HH:mm")
            .withZone(ZoneId.of("Europe/Moscow"));

    private final NotificationService notificationService;
    private final NotificationProperties notificationProperties;
    private final EventClient eventClient;

    /**
     * Отправляет напоминания о событиях, которые начнутся через 24-25 часов.
     * Запускается каждый час.
     */
    @Scheduled(cron = "${notification.reminder.cron:0 0 * * * *}")
    public void sendReminders() {
        log.info("Запуск планировщика напоминаний");

        try {
            Instant from = Instant.now().plus(24, ChronoUnit.HOURS);
            Instant to = from.plus(1, ChronoUnit.HOURS);

            // Получаем опубликованные события в диапазоне
            List<EventDto> events = eventClient.findUpcomingEvents(from, to);
            log.info("Найдено событий для напоминаний: count={}", events.size());

            int sentCount = 0;
            for (EventDto event : events) {
                // Дополнительная проверка статуса (на случай race condition)
                if (event.status() != EventStatus.PUBLISHED) {
                    continue;
                }

                // Получаем активные регистрации
                List<RegistrationDto> registrations =
                    eventClient.findActiveRegistrations(event.id(), event.tenantId());

                for (RegistrationDto reg : registrations) {
                    if (reg.status() == RegistrationStatus.CONFIRMED && reg.userId() != null) {
                        boolean sent = sendReminderToUser(reg, event);
                        if (sent) {
                            sentCount++;
                        }
                    }
                }
            }

            log.info("Планировщик напоминаний завершён: отправлено={}", sentCount);
        } catch (Exception e) {
            log.error("Ошибка в планировщике напоминаний: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет напоминание конкретному пользователю о регистрации.
     *
     * @param registration данные регистрации
     * @param event        данные события
     * @return true если уведомление отправлено
     */
    private boolean sendReminderToUser(RegistrationDto registration, EventDto event) {
        return sendReminder(
            registration.userId(),
            registration.firstName(),
            event.title(),
            event.slug(),
            event.startsAt(),
            event.locationAddress()
        );
    }

    /**
     * Отправляет напоминание участнику.
     *
     * @return true если уведомление отправлено
     */
    private boolean sendReminder(
            UUID userId,
            String firstName,
            String eventTitle,
            String eventSlug,
            Instant eventStartsAt,
            String locationAddress) {

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", firstName);
        variables.put("eventTitle", eventTitle);
        variables.put("eventDate", formatDate(eventStartsAt));
        variables.put("eventUrl", buildEventUrl(eventSlug));
        if (locationAddress != null) {
            variables.put("eventLocation", locationAddress);
        }

        boolean sent = notificationService.sendTelegram(
            userId,
            "event.reminder",
            variables,
            NotificationPreference.EVENT_REMINDER
        );

        if (sent) {
            log.debug("Напоминание отправлено: userId={}, event={}", userId, eventTitle);
        }

        return sent;
    }

    private String formatDate(Instant instant) {
        if (instant == null) {
            return "";
        }
        return DATE_FORMATTER.format(instant);
    }

    private String buildEventUrl(String eventSlug) {
        return notificationProperties.getEventUrl(eventSlug);
    }
}
