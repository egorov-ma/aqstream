package ru.aqstream.common.api.version;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

/**
 * Информация о версии отдельного сервиса.
 *
 * @param name             имя сервиса (например, "user-service")
 * @param version          версия приложения (например, "0.1.0-SNAPSHOT")
 * @param buildTime        время сборки
 * @param gitCommit        короткий hash Git коммита
 * @param gitBranch        ветка Git
 * @param gitCommitTime    время коммита
 * @param javaVersion      версия Java
 * @param springBootVersion версия Spring Boot
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ServiceVersionDto(
    String name,
    String version,
    Instant buildTime,
    String gitCommit,
    String gitBranch,
    Instant gitCommitTime,
    String javaVersion,
    String springBootVersion
) {

    /**
     * Создаёт краткую версию без деталей Git.
     *
     * @return DTO только с основной информацией
     */
    public ServiceVersionDto withoutGitDetails() {
        return new ServiceVersionDto(
            name, version, buildTime, null, null, null, javaVersion, springBootVersion
        );
    }

    /**
     * Создаёт DTO для недоступного сервиса.
     *
     * @param serviceName имя сервиса
     * @return DTO с версией "unavailable"
     */
    public static ServiceVersionDto unavailable(String serviceName) {
        return new ServiceVersionDto(
            serviceName,
            "unavailable",
            null, null, null, null, null, null
        );
    }
}
