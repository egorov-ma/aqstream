package ru.aqstream.event.client;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.aqstream.event.api.dto.EventDto;
import ru.aqstream.event.api.dto.RegistrationDto;

/**
 * Feign клиент для Event Service.
 * Используется другими сервисами для получения данных о событиях и регистрациях.
 */
@FeignClient(
    name = "event-service",
    url = "${event-service.url:http://localhost:8082}"
)
public interface EventClient {

    /**
     * Получает событие по ID.
     *
     * @param eventId ID события
     * @return данные события или пустой Optional
     */
    @GetMapping("/api/v1/internal/events/{eventId}")
    Optional<EventDto> findEventById(@PathVariable("eventId") UUID eventId);

    /**
     * Получает все активные регистрации события (статус CONFIRMED).
     * Используется для массовой рассылки уведомлений.
     *
     * @param eventId ID события
     * @return список регистраций
     */
    @GetMapping("/api/v1/internal/events/{eventId}/registrations")
    List<RegistrationDto> findActiveRegistrations(@PathVariable("eventId") UUID eventId);

    /**
     * Получает все активные регистрации события с указанным tenant.
     * Используется для массовой рассылки уведомлений.
     *
     * @param eventId  ID события
     * @param tenantId ID организации (для RLS)
     * @return список регистраций
     */
    @GetMapping("/api/v1/internal/events/{eventId}/registrations")
    List<RegistrationDto> findActiveRegistrations(
        @PathVariable("eventId") UUID eventId,
        @RequestParam("tenantId") UUID tenantId
    );
}
