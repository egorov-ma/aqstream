ARG BASE_IMAGE=ghcr.io/egorov-ma/aqstream/java-base:latest
FROM ${BASE_IMAGE}

LABEL description="Analytics Service для AqStream - сбор и анализ метрик"

COPY analytics-service-service/build/libs/analytics-service-service-*-boot.jar app.jar

RUN chown aqstream:aqstream app.jar

USER aqstream

EXPOSE 8086

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8086/actuator/health || exit 1
