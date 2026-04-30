# ----------- Stage 1: Build -----------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests


# ----------- Stage 2: Run -----------
FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

# Copy built JAR from the build stage
COPY --from=build /app/target/physiotherapydoctor-service-0.0.1-SNAPSHOT.jar /app/physiotherapydoctor-service.jar

# Expose port (must match application.properties)
EXPOSE 8888

# Run application
ENTRYPOINT ["java","-jar","physiotherapydoctor-service.jar"]