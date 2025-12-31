package ru.aqstream.event.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.aqstream.event.config.CacheConfig;
import ru.aqstream.user.client.UserClient;

/**
 * Сервис для получения названий организаций с кэшированием.
 * Использует user-service через Feign client.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationNameResolver {

    private final UserClient userClient;

    /**
     * Получает название организации по ID.
     * Результат кэшируется на 15 минут.
     *
     * @param organizationId идентификатор организации
     * @return название организации или null если не найдена
     */
    @Cacheable(value = CacheConfig.ORGANIZER_NAME_CACHE, key = "#organizationId", unless = "#result == null")
    public String resolve(UUID organizationId) {
        if (organizationId == null) {
            return null;
        }

        try {
            log.debug("Запрос названия организации: organizationId={}", organizationId);
            return userClient.findOrganizationById(organizationId)
                .map(org -> org.name())
                .orElse(null);
        } catch (Exception e) {
            log.warn("Не удалось получить название организации: organizationId={}, ошибка={}",
                organizationId, e.getMessage());
            return null;
        }
    }
}
