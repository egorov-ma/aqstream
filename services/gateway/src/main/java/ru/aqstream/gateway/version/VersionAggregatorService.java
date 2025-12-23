package ru.aqstream.gateway.version;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import ru.aqstream.common.api.version.ServiceVersionDto;
import ru.aqstream.common.api.version.SystemVersionDto;
import ru.aqstream.common.api.version.SystemVersionDto.InfrastructureVersionDto;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сервис агрегации версий всех микросервисов и инфраструктуры.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VersionAggregatorService {

    private final WebClient.Builder webClientBuilder;
    private final GatewayVersionProvider gatewayVersionProvider;
    private final InfrastructureVersionService infrastructureVersionService;
    private final ServiceEndpoints serviceEndpoints;

    @Value("${aqstream.environment:development}")
    private String environment;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * Агрегирует версии всех сервисов системы.
     *
     * @return системная информация о версиях
     */
    public Mono<SystemVersionDto> aggregateVersions() {
        // Параллельный запрос ко всем сервисам
        Mono<ServiceVersionDto> userService = fetchServiceVersion(
            "user-service", serviceEndpoints.userService());
        Mono<ServiceVersionDto> eventService = fetchServiceVersion(
            "event-service", serviceEndpoints.eventService());
        Mono<ServiceVersionDto> paymentService = fetchServiceVersion(
            "payment-service", serviceEndpoints.paymentService());
        Mono<ServiceVersionDto> notificationService = fetchServiceVersion(
            "notification-service", serviceEndpoints.notificationService());
        Mono<ServiceVersionDto> mediaService = fetchServiceVersion(
            "media-service", serviceEndpoints.mediaService());
        Mono<ServiceVersionDto> analyticsService = fetchServiceVersion(
            "analytics-service", serviceEndpoints.analyticsService());

        // Информация об инфраструктуре
        Mono<InfrastructureVersionDto> infrastructure = infrastructureVersionService.getInfrastructureVersions();

        return Mono.zip(
            userService,
            eventService,
            paymentService,
            notificationService,
            mediaService,
            analyticsService,
            infrastructure
        ).map(tuple -> {
            Map<String, ServiceVersionDto> services = new LinkedHashMap<>();
            services.put("user-service", tuple.getT1());
            services.put("event-service", tuple.getT2());
            services.put("payment-service", tuple.getT3());
            services.put("notification-service", tuple.getT4());
            services.put("media-service", tuple.getT5());
            services.put("analytics-service", tuple.getT6());

            return new SystemVersionDto(
                "AqStream",
                environment,
                Instant.now(),
                null, // frontend версия передаётся клиентом
                gatewayVersionProvider.getVersion(),
                services,
                tuple.getT7().hasAnyInfo() ? tuple.getT7() : null
            );
        });
    }

    private Mono<ServiceVersionDto> fetchServiceVersion(String serviceName, String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            log.debug("URL для {} не настроен", serviceName);
            return Mono.just(ServiceVersionDto.unavailable(serviceName));
        }

        return webClientBuilder.build()
            .get()
            .uri(ServiceEndpoints.versionUrl(baseUrl))
            .retrieve()
            .bodyToMono(ServiceVersionDto.class)
            .timeout(TIMEOUT)
            .doOnError(e -> log.warn("Не удалось получить версию {}: {}", serviceName, e.getMessage()))
            .onErrorResume(e -> Mono.just(ServiceVersionDto.unavailable(serviceName)));
    }
}
