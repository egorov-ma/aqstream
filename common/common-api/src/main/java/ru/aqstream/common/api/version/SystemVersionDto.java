package ru.aqstream.common.api.version;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

/**
 * Агрегированная информация о версиях всей системы AqStream.
 * Используется Gateway для предоставления сводной информации о версиях всех компонентов.
 *
 * @param platform       имя платформы ("AqStream")
 * @param environment    окружение (development, staging, production)
 * @param timestamp      время запроса
 * @param frontend       версия фронтенда (если передана)
 * @param gateway        версия Gateway
 * @param services       версии всех микросервисов
 * @param infrastructure информация об инфраструктурных компонентах
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SystemVersionDto(
    String platform,
    String environment,
    Instant timestamp,
    ServiceVersionDto frontend,
    ServiceVersionDto gateway,
    Map<String, ServiceVersionDto> services,
    InfrastructureVersionDto infrastructure
) {

    /**
     * Информация о версиях инфраструктурных компонентов.
     *
     * @param postgresql версия PostgreSQL
     * @param redis      версия Redis
     * @param rabbitmq   версия RabbitMQ
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InfrastructureVersionDto(
        String postgresql,
        String redis,
        String rabbitmq
    ) {

        /**
         * Проверяет, есть ли информация хотя бы об одном компоненте.
         *
         * @return true если есть информация
         */
        public boolean hasAnyInfo() {
            return postgresql != null || redis != null || rabbitmq != null;
        }
    }
}
