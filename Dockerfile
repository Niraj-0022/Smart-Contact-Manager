# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:25-jdk AS builder

WORKDIR /app

# Copy Maven wrapper
COPY mvnw .
COPY .mvn .mvn

# Copy Maven configuration
COPY pom.xml .

# Copy source code
COPY src src

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the Spring Boot JAR
RUN ./mvnw clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy generated JAR
COPY --from=builder /app/target/*.jar app.jar

# Spring Boot port
EXPOSE 8080

# Start application
ENTRYPOINT ["java", "-jar", "app.jar"]
