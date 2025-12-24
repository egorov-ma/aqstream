FROM eclipse-temurin:25-jre-alpine

# Метаданные
LABEL maintainer="AqStream Team" \
      description="Base image for AqStream Java microservices"

# Создаём непривилегированного пользователя
RUN addgroup -S aqstream && adduser -S aqstream -G aqstream

WORKDIR /app

# JVM опции для контейнеров
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# Дочерние образы должны:
# 1. COPY JAR файл как app.jar
# 2. RUN chown aqstream:aqstream app.jar
# 3. USER aqstream
# 4. EXPOSE port
# 5. HEALTHCHECK

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
