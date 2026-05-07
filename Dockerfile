# syntax=docker/dockerfile:1.6
# ============================================================================
# Build stage — Maven + JDK 21, fully offline-friendly cache by copying pom
# first.
# ============================================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw mvnw.cmd ./
RUN mvn -B -e -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -e -DskipTests package

# ============================================================================
# Runtime stage — slim JRE on Alpine; non-root user; healthcheck wired to
# /actuator/health/readiness so orchestrators can probe.
# ============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# wget is used by the HEALTHCHECK below; it's already in alpine's default image.
RUN addgroup -S cardapio && adduser -S -G cardapio cardapio

COPY --from=build /workspace/target/*.jar /app/app.jar

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health/readiness || exit 1

USER cardapio
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
