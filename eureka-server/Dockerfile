# Stage 1: Build the application with Maven using the root context
# WARNING: This service uses Java 17, while other services use Java 21. This should be unified later.
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Copy the entire project
COPY . .
# Build only the eureka-server module and its dependencies
RUN mvn -pl eureka-server -am clean install -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
# Copy the built JAR file from the specific target folder in the build stage
COPY --from=build /app/eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar ./app.jar
# Expose the port the application runs on
EXPOSE 8761
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
