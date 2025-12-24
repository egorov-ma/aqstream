ARG BASE_IMAGE=ghcr.io/egorov-ma/aqstream/java-base:latest
FROM ${BASE_IMAGE}

LABEL description="API Gateway для AqStream"

COPY build/libs/gateway-*-boot.jar app.jar

RUN chown aqstream:aqstream app.jar

USER aqstream

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
