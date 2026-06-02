FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy the pre-built jar from the host target directory
COPY target/fpm-monolith-1.0.0-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
