# Stage 1: Build the application with Maven using the root context
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Copy the entire project
COPY . .
# Build only the ocr-service module and its dependencies
RUN mvn -pl ocr-service -am clean install -DskipTests

# Stage 2: Create the final lightweight image
FROM eclipse-temurin:21-jre-jammy
# Install Tesseract for OCR and Vietnamese language data
RUN apt-get update && apt-get install -y \
    tesseract-ocr \
    tesseract-ocr-vie \
    tesseract-ocr-eng \
    libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

# Set TESSDATA_PREFIX environment variable
ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata

WORKDIR /app
# Copy the built JAR file from the specific target folder in the build stage
COPY --from=build /app/ocr-service/target/ocr-service-0.0.1-SNAPSHOT.jar ./app.jar
# Expose the port the application runs on
EXPOSE 8086
# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
