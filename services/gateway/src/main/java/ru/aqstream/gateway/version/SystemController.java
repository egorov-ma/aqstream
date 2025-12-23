package ru.aqstream.gateway.version;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.aqstream.common.api.version.ServiceVersionDto;
import ru.aqstream.common.api.version.SystemVersionDto;

/**
 * Контроллер системной информации Gateway.
 * Предоставляет агрегированную информацию о версиях всех компонентов системы.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Системная информация")
public class SystemController {

    private final VersionAggregatorService versionAggregatorService;
    private final GatewayVersionProvider gatewayVersionProvider;

    /**
     * Возвращает агрегированные версии всех сервисов системы.
     * Используется фронтендом для отображения в консоли.
     *
     * @return информация о версиях всех компонентов
     */
    @Operation(
        summary = "Получить версии всех сервисов",
        description = "Агрегирует информацию о версиях Gateway, всех микросервисов и инфраструктуры"
    )
    @ApiResponse(responseCode = "200", description = "Информация о версиях")
    @GetMapping("/version")
    public Mono<SystemVersionDto> getSystemVersion() {
        return versionAggregatorService.aggregateVersions();
    }

    /**
     * Возвращает только версию Gateway (быстрый endpoint).
     *
     * @return информация о версии Gateway
     */
    @Operation(
        summary = "Получить версию Gateway",
        description = "Возвращает только информацию о версии Gateway без опроса других сервисов"
    )
    @ApiResponse(responseCode = "200", description = "Информация о версии Gateway")
    @GetMapping("/version/gateway")
    public Mono<ServiceVersionDto> getGatewayVersion() {
        return Mono.just(gatewayVersionProvider.getVersion());
    }
}
