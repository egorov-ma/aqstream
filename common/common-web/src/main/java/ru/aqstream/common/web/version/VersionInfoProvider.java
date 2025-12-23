package ru.aqstream.common.web.version;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;
import org.springframework.stereotype.Component;
import ru.aqstream.common.api.version.ServiceVersionDto;

import java.time.Instant;
import java.util.Optional;

/**
 * Провайдер информации о версии сервиса.
 * Собирает данные из build-info.properties и git.properties.
 */
@Component
@Slf4j
public class VersionInfoProvider {

    private final Optional<BuildProperties> buildProperties;
    private final Optional<GitProperties> gitProperties;

    public VersionInfoProvider(
            Optional<BuildProperties> buildProperties,
            Optional<GitProperties> gitProperties
    ) {
        this.buildProperties = buildProperties;
        this.gitProperties = gitProperties;

        if (buildProperties.isEmpty()) {
            log.warn("BuildProperties недоступны. Убедитесь, что springBoot.buildInfo() настроен.");
        }
        if (gitProperties.isEmpty()) {
            log.debug("GitProperties недоступны. Git информация не будет включена в версию.");
        }
    }

    /**
     * Возвращает полную информацию о версии сервиса.
     *
     * @return DTO с информацией о версии
     */
    public ServiceVersionDto getVersion() {
        return new ServiceVersionDto(
            getName(),
            getVersionString(),
            getBuildTime(),
            getGitCommit(),
            getGitBranch(),
            getGitCommitTime(),
            System.getProperty("java.version"),
            getSpringBootVersion()
        );
    }

    private String getName() {
        return buildProperties
            .map(BuildProperties::getName)
            .orElse("unknown");
    }

    private String getVersionString() {
        return buildProperties
            .map(BuildProperties::getVersion)
            .orElse("unknown");
    }

    private Instant getBuildTime() {
        return buildProperties
            .map(BuildProperties::getTime)
            .orElse(null);
    }

    private String getGitCommit() {
        return gitProperties
            .map(GitProperties::getShortCommitId)
            .orElse(null);
    }

    private String getGitBranch() {
        return gitProperties
            .map(GitProperties::getBranch)
            .orElse(null);
    }

    private Instant getGitCommitTime() {
        return gitProperties
            .map(GitProperties::getCommitTime)
            .orElse(null);
    }

    private String getSpringBootVersion() {
        return buildProperties
            .map(p -> p.get("spring-boot.version"))
            .orElseGet(() -> org.springframework.boot.SpringBootVersion.getVersion());
    }
}
