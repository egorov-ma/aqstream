ARG BASE_IMAGE=ghcr.io/egorov-ma/aqstream/java-base:latest
FROM ${BASE_IMAGE}

LABEL description="Media Service для AqStream - загрузка и обработка файлов"

COPY media-service-service/build/libs/media-service-service-*-boot.jar app.jar

RUN chown aqstream:aqstream app.jar

USER aqstream

EXPOSE 8085

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1
