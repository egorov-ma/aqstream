ARG BASE_IMAGE=ghcr.io/egorov-ma/aqstream/java-base:latest
FROM ${BASE_IMAGE}

LABEL description="Payment Service для AqStream - обработка платежей и возвратов"

COPY payment-service-service/build/libs/payment-service-service-*-boot.jar app.jar

RUN chown aqstream:aqstream app.jar

USER aqstream

EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1
