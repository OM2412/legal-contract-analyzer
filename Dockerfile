# Build the React frontend.
FROM node:24-bookworm-slim AS frontend-build

WORKDIR /frontend
ARG VITE_AI_ENABLED=false
ENV VITE_AI_ENABLED=${VITE_AI_ENABLED}

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


# Run backend tests and package the React build.
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


# Run the packaged application with OCR support.
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

COPY scripts/docker-entrypoint.sh /app/docker-entrypoint.sh

# Ensure the shell script runs even if Windows saved it with CRLF endings.
RUN sed -i 's/\r$//' /app/docker-entrypoint.sh

USER 10001

ENTRYPOINT ["sh", "/app/docker-entrypoint.sh"]