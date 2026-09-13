# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 – Build
# Uses the full Maven + JDK image to compile and package the app.
# The .mvn wrapper and pom.xml are copied first so Docker can cache the
# dependency-download layer and skip it on re-builds when only source changes.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy dependency descriptors first (cache-friendly)
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./

# Pre-download all dependencies (this layer is cached until pom.xml changes)
RUN mvn dependency:go-offline -q

# Copy source and build the fat JAR, skipping tests
# (tests should run in CI, not inside the Docker build)
COPY src/ src/
RUN mvn clean package -Dmaven.test.skip=true -q


# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 – Runtime
# Uses a minimal JRE-only image — no compiler, no Maven, much smaller surface.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user so the app doesn't run as root inside the container
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the packaged JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

# Render injects PORT automatically; default to 8080 for local docker runs
EXPOSE 8080

# Use exec form so signals (SIGTERM) reach the JVM directly for graceful shutdown
ENTRYPOINT ["java", "-jar", "app.jar"]
