# ============================
# 1. Stage: сборка jar через Gradle Wrapper
# ============================
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app

# Копируем wrapper и gradle конфиги (для кэширования зависимостей)
COPY gradlew .
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY payments/build.gradle payments/build.gradle
COPY api api

# Скачиваем зависимости чтобы кэшировались
RUN chmod +x gradlew
RUN ./gradlew :payments:dependencies --no-daemon || true

# Копируем исходники
COPY payments/src payments/src

# Собираем jar
RUN ./gradlew :payments:bootJar --no-daemon

# ============================
# 2. Stage: минимальный runtime
# ============================
FROM eclipse-temurin:21-jre
WORKDIR /app

# Копируем только готовый jar из builder
COPY --from=builder /app/payments/build/libs/payments_service.jar app.jar

EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
