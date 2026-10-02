# Build the React frontend.
FROM node:24-bookworm-slim AS frontend-build

WORKDIR /frontend
ARG VITE_AI_ENABLED=false
ENV VITE_AI_ENABLED=${VITE_AI_ENABLED}

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


# Run backend tests and package the React build in the Spring Boot JAR.
FROM maven:3.9.16-eclipse-temurin-25-noble AS backend-build

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
ENV FOLIO_AI_ENABLED=false

COPY pom.xml ./
COPY src ./src
COPY testdata ./testdata
COPY --from=frontend-build /frontend/dist ./frontend/dist

RUN mvn -B package


# Keep only the Java runtime, application JAR, and OCR tools.
FROM eclipse-temurin:25-jre-noble

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        tesseract-ocr \
        tesseract-ocr-eng \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=backend-build \
    /app/target/legal-contract-analyzer-0.1.0-SNAPSHOT-exec.jar \
    /app/app.jar

USER 10001

ENTRYPOINT ["sh", "-c", "exec java -jar /app/app.jar --server.address=0.0.0.0 --server.port=${PORT:-8080} --folio.ai.enabled=${FOLIO_AI_ENABLED:-false}"]