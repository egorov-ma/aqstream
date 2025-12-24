ARG BASE_IMAGE=ghcr.io/egorov-ma/aqstream/java-base:latest
FROM ${BASE_IMAGE}

LABEL description="Event Service для AqStream - управление событиями, регистрациями и check-in"

COPY event-service-service/build/libs/event-service-service-*-boot.jar app.jar

RUN chown aqstream:aqstream app.jar

USER aqstream

EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1
