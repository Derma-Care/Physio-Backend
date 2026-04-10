# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy Maven project files
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy application source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Create a minimal runtime image
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy built JAR from the build stage
COPY --from=build /app/target/customerservice-0.0.1-SNAPSHOT.jar /app/customerservice.jar

# Define container port
EXPOSE 8083

# Run the application
ENTRYPOINT ["java", "-jar", "customerservice.jar"]
