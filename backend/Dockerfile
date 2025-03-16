# Build Stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
LABEL authors="ogeedeveloper"

# Set working directory
WORKDIR /app

# Copy Maven dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime Stage
FROM openjdk:21-jdk AS runner

WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /app/target/realtime-emergency-response-alert-system-0.0.1-SNAPSHOT.jar app.jar

# Expose port
EXPOSE 4000

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
