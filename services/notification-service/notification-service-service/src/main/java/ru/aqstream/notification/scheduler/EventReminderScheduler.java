package ru.aqstream.notification.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.aqstream.notification.config.NotificationProperties;
import ru.aqstream.notification.db.entity.NotificationPreference;
import ru.aqstream.notification.service.NotificationService;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

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
    // TODO: Добавить EventClient для получения событий и регистраций
    // private final EventClient eventClient;

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

            // TODO: Реализовать получение событий через EventClient
            // List<EventSummaryDto> events = eventClient.findByStartsAtBetween(from, to);
            //
            // for (EventSummaryDto event : events) {
            //     if (event.status() != EventStatus.PUBLISHED) continue;
            //
            //     List<RegistrationSummaryDto> registrations =
            //         eventClient.findRegistrationsByEventId(event.id());
            //
            //     for (RegistrationSummaryDto reg : registrations) {
            //         if (reg.status() == RegistrationStatus.CONFIRMED) {
            //             sendReminder(reg, event);
            //         }
            //     }
            // }

            log.info("Планировщик напоминаний завершён");
        } catch (Exception e) {
            log.error("Ошибка в планировщике напоминаний: {}", e.getMessage(), e);
        }
    }

    /**
     * Отправляет напоминание участнику.
     */
    private void sendReminder(
            java.util.UUID userId,
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
