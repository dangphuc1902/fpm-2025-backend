FROM eclipse-temurin:21-jre-jammy

# Install Tesseract OCR native binaries and language packs
RUN apt-get update && \
    apt-get install -y --no-install-recommends tesseract-ocr tesseract-ocr-vie tesseract-ocr-eng && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the pre-built jar from the host target directory
COPY target/fpm-monolith-1.0.0-SNAPSHOT.jar app.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
