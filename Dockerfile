# ---------------------------------------------------------------------------
# Stage 1: Build
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /workspace

COPY gradle gradle
COPY gradlew .
COPY settings.gradle .
COPY build.gradle .
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon -q

# ---------------------------------------------------------------------------
# Stage 2: Run
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=build --chown=appuser:appgroup /workspace/build/libs/*.jar app.jar

# Default environment (overridden at deploy time)
ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS=""

EXPOSE 8080
# Actuator management port (prod profile binds here)
EXPOSE 8081

USER appuser

# Spring Boot liveness/readiness probes via Actuator
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://localhost:${SERVER_PORT}/actuator/health/liveness || exit 1

# Use exec form so Java receives SIGTERM directly (graceful shutdown).
# JAVA_OPTS allows injecting JVM flags at runtime without rebuilding the image.
ENTRYPOINT ["sh", "-c", \
  "exec java \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -Djava.security.egd=file:/dev/./urandom \
    $JAVA_OPTS \
    -jar /app/app.jar"]
