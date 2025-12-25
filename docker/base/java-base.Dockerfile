FROM eclipse-temurin:25-jre-alpine

# Метаданные
LABEL maintainer="AqStream Team" \
      description="Base image for AqStream Java microservices"

# Создаём непривилегированного пользователя
RUN addgroup -S aqstream && adduser -S aqstream -G aqstream

WORKDIR /app

# JVM опции для контейнеров (оптимизировано для 1vCPU/2GB)
# - MaxRAMPercentage=60: оставляет 40% для JVM overhead
# - UseSerialGC: меньше памяти чем G1GC для малых heap
# - TieredStopAtLevel=1: быстрый старт, меньше JIT памяти
# - Xss256k: уменьшает размер стека потоков
ENV JAVA_OPTS="-XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=60.0 \
    -XX:InitialRAMPercentage=40.0 \
    -XX:+UseSerialGC \
    -XX:+TieredCompilation \
    -XX:TieredStopAtLevel=1 \
    -Xss256k \
    -Djava.security.egd=file:/dev/./urandom"

# Дочерние образы должны:
# 1. COPY JAR файл как app.jar
# 2. RUN chown aqstream:aqstream app.jar
# 3. USER aqstream
# 4. EXPOSE port
# 5. HEALTHCHECK

# Запуск приложения
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
