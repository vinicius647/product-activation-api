# syntax=docker/dockerfile:1

# ---- Build stage: compiles the jar. Tests run in CI (./mvnw verify), not here.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY app/pom.xml app/pom.xml
COPY core/src core/src
COPY app/src app/src
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -DskipTests package

# ---- Runtime stage: only the JRE and the jar, running as a non-root user.
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build /workspace/app/target/product-activation-api.jar app.jar
USER app

# The JVM uses a percentage of the container memory limit instead of the host memory.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/liveness || exit 1

# exec form: the JVM is PID 1 and receives SIGTERM, so the graceful shutdown runs.
ENTRYPOINT ["java", "-jar", "app.jar"]
