# Build Stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
LABEL maintainer="ogeedeveloper"

WORKDIR /app

# Copy the pom.xml and download dependencies in parallel
COPY pom.xml .
RUN mvn -B -T 1C dependency:go-offline

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM openjdk:21-jdk-slim
LABEL maintainer="ogeedeveloper"

WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /app/target/realtime-emergency-response-alert-system-0.0.1-SNAPSHOT.jar app.jar

# Create a non-root user for better security
RUN useradd -ms /bin/bash appuser && chown -R appuser /app
USER appuser

# Expose the application port
EXPOSE 4000

# Start the application
CMD ["java", "-jar", "app.jar"]
