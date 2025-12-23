package ru.aqstream.common.web.version;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.api.version.ServiceVersionDto;

/**
 * Контроллер для получения информации о версии сервиса.
 * Публичный endpoint без аутентификации.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Системная информация")
public class VersionController {

    private final VersionInfoProvider versionInfoProvider;

    /**
     * Возвращает информацию о версии сервиса.
     *
     * @return информация о версии, сборке и Git коммите
     */
    @Operation(
        summary = "Получить версию сервиса",
        description = "Возвращает информацию о версии приложения, времени сборки и Git коммите"
    )
    @ApiResponse(responseCode = "200", description = "Информация о версии")
    @GetMapping("/version")
    public ServiceVersionDto getVersion() {
        return versionInfoProvider.getVersion();
    }
}
