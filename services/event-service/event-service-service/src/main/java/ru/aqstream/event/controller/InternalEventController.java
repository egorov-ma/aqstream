package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.security.TenantContext;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.RegistrationDto;
import ru.aqstream.event.db.repository.EventRepository;
import ru.aqstream.event.db.repository.RegistrationRepository;
import ru.aqstream.event.service.EventMapper;
import ru.aqstream.event.service.RegistrationMapper;
import ru.aqstream.event.service.TicketImageService;

/**
 * Внутренний контроллер для межсервисного взаимодействия.
 * Используется другими сервисами через Feign клиенты.
 *
 * <p>Эндпоинты не требуют аутентификации пользователя,
 * но должны быть защищены на уровне сети (внутренняя сеть).</p>
 *
 * <p>ВАЖНО: tenantId передаётся как параметр для установки TenantContext,
 * что необходимо для корректной работы RLS.</p>
 */
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Internal Events", description = "Внутренние эндпоинты для межсервисного взаимодействия")
public class InternalEventController {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final RegistrationMapper registrationMapper;
    private final TicketImageService ticketImageService;

    /**
     * Получает событие по ID.
     *
     * @param eventId  ID события
     * @param tenantId ID организации (для RLS)
     * @return данные события или 404
     */
    @Operation(summary = "Получить событие по ID", description = "Внутренний эндпоинт для других сервисов")
    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventDto> findEventById(
        @PathVariable UUID eventId,
        @RequestParam(required = false) UUID tenantId
    ) {
        log.debug("Internal: запрос события: eventId={}, tenantId={}", eventId, tenantId);

        try {
            // Устанавливаем TenantContext если передан tenantId
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }

            return eventRepository.findByIdAndTenantId(eventId, tenantId)
                .map(eventMapper::toDto)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
        } finally {
            // Очищаем контекст
            if (tenantId != null) {
                TenantContext.clear();
            }
        }
    }

    /**
     * Получает все активные регистрации события (статус != CANCELLED).
     * Используется для массовой рассылки уведомлений при отмене события.
     *
     * @param eventId  ID события
     * @param tenantId ID организации (для RLS)
     * @return список активных регистраций
     */
    @Operation(
        summary = "Получить активные регистрации события",
        description = "Возвращает все регистрации со статусом != CANCELLED для массовой рассылки"
    )
    @GetMapping("/events/{eventId}/registrations")
    public ResponseEntity<List<RegistrationDto>> findActiveRegistrations(
        @PathVariable UUID eventId,
        @RequestParam(required = false) UUID tenantId
    ) {
        log.debug("Internal: запрос регистраций события: eventId={}, tenantId={}", eventId, tenantId);

        try {
            // Устанавливаем TenantContext если передан tenantId
            if (tenantId != null) {
                TenantContext.setTenantId(tenantId);
            }

            List<RegistrationDto> registrations = registrationRepository.findActiveByEventId(eventId)
                .stream()
                .map(registrationMapper::toDto)
                .toList();

            log.debug("Internal: найдено регистраций: eventId={}, count={}", eventId, registrations.size());

            return ResponseEntity.ok(registrations);
        } finally {
            // Очищаем контекст
            if (tenantId != null) {
                TenantContext.clear();
            }
        }
    }

    /**
     * Получает опубликованные события, которые начнутся в указанном диапазоне времени.
     * Используется планировщиком напоминаний.
     *
     * <p>Возвращает события всех организаций (без RLS) для отправки напоминаний.</p>
     *
     * @param from начало диапазона (включительно)
     * @param to   конец диапазона (не включительно)
     * @return список опубликованных событий
     */
    @Operation(
        summary = "Получить предстоящие события",
        description = "Возвращает опубликованные события в указанном временном диапазоне"
    )
    @GetMapping("/events/upcoming")
    public ResponseEntity<List<EventDto>> findUpcomingEvents(
        @RequestParam Instant from,
        @RequestParam Instant to
    ) {
        log.debug("Internal: запрос предстоящих событий: from={}, to={}", from, to);

        List<EventDto> events = eventRepository.findPublishedByStartsAtBetween(from, to)
            .stream()
            .map(eventMapper::toDto)
            .toList();

        log.info("Internal: найдено предстоящих событий: count={}", events.size());

        return ResponseEntity.ok(events);
    }

    /**
     * Получает регистрацию по ID.
     * Используется Telegram ботом для отображения билета.
     *
     * <p>Возвращает регистрацию без проверки RLS (нужна для deeplink /start reg_{id}).</p>
     *
     * @param registrationId ID регистрации
     * @return данные регистрации или 404
     */
    @Operation(
        summary = "Получить регистрацию по ID",
        description = "Внутренний эндпоинт для Telegram бота"
    )
    @GetMapping("/registrations/{registrationId}")
    public ResponseEntity<RegistrationDto> findRegistrationById(
        @PathVariable UUID registrationId
    ) {
        log.debug("Internal: запрос регистрации: registrationId={}", registrationId);

        return registrationRepository.findById(registrationId)
            .map(registrationMapper::toDto)
            .map(ResponseEntity::ok)
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Генерирует изображение билета для регистрации.
     * Используется Notification Service для отправки билета в Telegram.
     *
     * @param registrationId ID регистрации
     * @return PNG изображение билета или 404
     */
    @Operation(
        summary = "Получить изображение билета",
        description = "Генерирует PNG изображение билета с QR-кодом для отправки в Telegram"
    )
    @GetMapping(value = "/registrations/{registrationId}/ticket-image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getTicketImage(@PathVariable UUID registrationId) {
        log.debug("Internal: запрос изображения билета: registrationId={}", registrationId);

        return registrationRepository.findById(registrationId)
            .map(registration -> {
                byte[] ticketImage = ticketImageService.generateTicketImage(registration);
                log.info("Internal: билет сгенерирован: registrationId={}, bytes={}",
                    registrationId, ticketImage.length);
                return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(ticketImage);
            })
            .orElseGet(() -> {
                log.warn("Internal: регистрация не найдена: registrationId={}", registrationId);
                return ResponseEntity.notFound().build();
            });
    }
}
