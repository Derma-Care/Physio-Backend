# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven project files and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy application source code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Create a minimal runtime image
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy built JAR from the build stage
COPY --from=build /app/target/notification-service-0.0.1-SNAPSHOT.jar /app/notification-service.jar

# Expose the application port
EXPOSE 8089

# Run the application
ENTRYPOINT ["java", "-jar", "notification-service.jar"]
