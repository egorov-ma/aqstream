FROM eclipse-temurin:25-jre-alpine

# Метаданные
LABEL maintainer="AqStream Team" \
      description="User Service для AqStream - управление пользователями, организациями и аутентификацией"

# Создаём непривилегированного пользователя
RUN addgroup -S aqstream && adduser -S aqstream -G aqstream

WORKDIR /app

# Копируем JAR файл
COPY user-service-service/build/libs/user-service-service-*-boot.jar app.jar

# Устанавливаем владельца
RUN chown -R aqstream:aqstream /app

# Переключаемся на непривилегированного пользователя
USER aqstream

# Expose порт
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# Запуск приложения
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar"]
