# Build the application with JDK 21 in a dedicated build stage.
FROM eclipse-temurin:21-jdk-alpine AS build

# Use a consistent directory for all build files.
WORKDIR /app

# Copy Maven Wrapper files and the project descriptor first to improve layer caching.
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Download dependencies before copying the source code so this layer can be reused.
RUN ./mvnw -B -ntp dependency:go-offline

# Copy the application sources and build the executable JAR.
# Tests run as a separate CI step because integration tests require Testcontainers.
COPY src ./src
RUN ./mvnw -B -ntp package -DskipTests

# Run the application in a smaller Java 21 runtime image.
FROM eclipse-temurin:21-jre-alpine

# Create a dedicated non-root user and group for improved container security.
RUN addgroup -S -g 1001 spring \
    && adduser -S -D -H -u 1001 -G spring spring \
    && mkdir -p /app \
    && chown spring:spring /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Run all subsequent commands and the application as the non-root user.
USER spring:spring

# Set the application directory inside the runtime container.
WORKDIR /app

# Copy only the executable JAR from the build stage into the final image.
COPY --from=build --chown=spring:spring /app/target/device-management-api-*.jar app.jar

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health/readiness >/dev/null || exit 1

# Document the HTTP port exposed by the Spring Boot application.
EXPOSE 8080

# Params
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

# Start the application and limit the Java heap to 75% of available container memory.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
