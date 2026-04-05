# Stage 1: Build the application with Maven using the root context
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the entire project
COPY . .
# Build only the transaction-service module and its dependencies
RUN mvn -pl transaction-service -am clean install -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copy the built JAR file from the specific target folder in the build stage
COPY --from=build /app/transaction-service/target/transaction-service-0.0.1-SNAPSHOT.jar ./app.jar
# Expose the ports the application runs on (Web and gRPC)
EXPOSE 8083 9093
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
