package ru.aqstream.event.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
}
