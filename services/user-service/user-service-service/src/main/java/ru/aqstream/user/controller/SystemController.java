package ru.aqstream.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.aqstream.common.api.version.ServiceVersionDto;

/**
 * Контроллер системной информации User Service.
 * Предоставляет информацию о версии сервиса.
 */
@RestController
@RequestMapping("/api/v1/system")
@RequiredArgsConstructor
@Tag(name = "System", description = "Системная информация")
public class SystemController {

    private final BuildProperties buildProperties;

    /**
     * Возвращает информацию о версии сервиса.
     * Используется Gateway для агрегации версий всей системы.
     *
     * @return информация о версии User Service
     */
    @Operation(
        summary = "Получить версию сервиса",
        description = "Возвращает информацию о версии User Service (build time, git commit, версии)"
    )
    @ApiResponse(responseCode = "200", description = "Информация о версии")
    @GetMapping("/version")
    public ResponseEntity<ServiceVersionDto> getVersion() {
        ServiceVersionDto version = new ServiceVersionDto(
            buildProperties.getName(),
            buildProperties.getVersion(),
            buildProperties.getTime(),
            buildProperties.get("git.commit.id.abbrev"),
            buildProperties.get("git.branch"),
            parseInstant(buildProperties.get("git.commit.time")),
            buildProperties.get("java.version"),
            buildProperties.get("spring-boot.version")
        );

        return ResponseEntity.ok(version);
    }

    private java.time.Instant parseInstant(String isoString) {
        if (isoString == null || isoString.isBlank()) {
            return null;
        }
        try {
            return java.time.Instant.parse(isoString);
        } catch (Exception e) {
            return null;
        }
    }
}
