# ============================================
# Build stage — Gradle ビルド用
# ============================================
FROM gradle:8.12-jdk21-alpine AS build
WORKDIR /app
COPY build.gradle settings.gradle gradle.properties ./
RUN gradle dependencies --no-daemon || true
COPY src ./src
RUN gradle bootJar --no-daemon

# ============================================
# Dev stage — オンデマンド実行
# ============================================
FROM gradle:8.12-jdk21-alpine AS dev
WORKDIR /app
CMD ["gradle", "bootRun", "--no-daemon"]

# ============================================
# Prod stage — JRE のみで JAR 実行
# ============================================
FROM eclipse-temurin:21-jre-alpine AS prod
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
