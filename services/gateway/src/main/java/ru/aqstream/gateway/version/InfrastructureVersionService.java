package ru.aqstream.gateway.version;

import io.lettuce.core.api.StatefulRedisConnection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.aqstream.common.api.version.SystemVersionDto.InfrastructureVersionDto;

/**
 * Сервис для получения версий инфраструктурных компонентов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InfrastructureVersionService {

    private final ReactiveRedisConnectionFactory redisConnectionFactory;

    /**
     * Получает версии инфраструктурных компонентов.
     * PostgreSQL и RabbitMQ версии получаются от downstream сервисов.
     *
     * @return информация о версиях инфраструктуры
     */
    public Mono<InfrastructureVersionDto> getInfrastructureVersions() {
        return getRedisVersion()
            .map(redisVersion -> new InfrastructureVersionDto(
                null, // PostgreSQL версия получается из actuator/info сервисов
                redisVersion,
                null  // RabbitMQ версия получается из actuator/info сервисов
            ))
            .onErrorResume(e -> {
                log.warn("Не удалось получить версию Redis: {}", e.getMessage());
                return Mono.just(new InfrastructureVersionDto(null, null, null));
            });
    }

    private Mono<String> getRedisVersion() {
        return redisConnectionFactory.getReactiveConnection()
            .serverCommands()
            .info("server")
            .map(this::parseRedisVersion)
            .onErrorResume(e -> {
                log.debug("Ошибка получения версии Redis: {}", e.getMessage());
                return Mono.empty();
            });
    }

    private String parseRedisVersion(java.util.Properties properties) {
        String version = properties.getProperty("redis_version");
        if (version != null) {
            return version;
        }
        // Fallback: попробуем найти в строковом представлении
        String info = properties.toString();
        if (info.contains("redis_version:")) {
            int start = info.indexOf("redis_version:") + 14;
            int end = info.indexOf(",", start);
            if (end == -1) {
                end = info.indexOf("}", start);
            }
            return info.substring(start, end).trim();
        }
        return null;
    }
}
