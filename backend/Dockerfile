FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
LABEL authors="ogeedeveloper"

# Specifiy working directory
WORKDIR /app

COPY pom.xml .

# Download dependencies
RUN mvn -B dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package

# Instruction to run the build application
FROM openjdk:21-jdk AS runner

# Specifiy working directory
WORKDIR /app

# Copy the build artifact from the builder image
COPY --from=builder /app/target/realtime-emergency-response-alert-system-0.0.1-SNAPSHOT.jar .

# Export the port
EXPOSE 4002

# Run the application
ENTRYPOINT ["java", "-jar", "realtime-emergency-response-alert-system-0.0.1-SNAPSHOT.jar"]