# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY user-auth-service user-auth-service
COPY ocr-service ocr-service
COPY ai-service ai-service
COPY notification-service notification-service
COPY wallet-service wallet-service
COPY reporting-service reporting-service
COPY transaction-service transaction-service
COPY fpm-monolith fpm-monolith

# Compile and package only the monolith executable
RUN mvn clean package -pl fpm-monolith -am -DskipTests

# Stage 2: Runtime JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/fpm-monolith/target/fpm-monolith-1.0.0-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
