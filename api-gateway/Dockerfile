# Stage 1: Build the application with Maven using the root context
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the entire project
COPY . .
# Build only the api-gateway module and its dependencies
RUN mvn -pl api-gateway -am clean install -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built JAR file from the specific target folder in the build stage
COPY --from=build /app/api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar ./app.jar
# Expose the port the application runs on
EXPOSE 8080
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
