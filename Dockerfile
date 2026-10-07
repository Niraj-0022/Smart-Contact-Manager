# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:17-jdk-focal AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper files
COPY mvnw .
COPY .mvn .mvn

# Copy Maven project configuration
COPY pom.xml .

# Copy source code
COPY src src

# Make Maven wrapper executable
RUN chmod +x mvnw

# Build the Spring Boot executable JAR
# Tests are skipped during Docker build
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:17-jre-focal

# Set working directory
WORKDIR /app

# Copy the generated JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Railway/Render can provide the PORT environment variable
EXPOSE 8080

# Start Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
