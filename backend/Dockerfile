# Build stage
FROM eclipse-temurin:25-jdk-alpine AS builder

WORKDIR /build

# Copy Maven wrapper and pom.xml first for dependency caching
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application, skipping tests
RUN ./mvnw -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre-alpine

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

# Copy the JAR from build stage
COPY --from=builder /build/target/*.jar app.jar

# Change ownership to non-root user
RUN chown spring:spring app.jar

USER spring

# Set JVM options for container environment
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

# Expose port (will be overridden by PORT env var in application.properties)
EXPOSE 8080

# Use exec form to ensure proper signal handling
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]