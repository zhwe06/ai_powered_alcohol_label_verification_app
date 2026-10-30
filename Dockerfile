# ---------- Stage 1: build the React frontend ----------
FROM node:24-alpine AS frontend
WORKDIR /app/frontend

# Copy only the dependency files first so this layer is cached
# until package.json / package-lock.json actually change.
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci

COPY frontend/ ./
RUN npm run build


# ---------- Stage 2: build the Spring Boot jar ----------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend

COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline

COPY backend/src ./src
# Bundle the built frontend so Spring Boot serves it from /static.
COPY --from=frontend /app/frontend/dist ./src/main/resources/static
RUN mvn -q -DskipTests package


# ---------- Stage 3: runtime image ----------
FROM eclipse-temurin:21-jre

# Tess4J only ships Windows native libraries; install Tesseract for Linux.
RUN apt-get update \
    && apt-get install -y --no-install-recommends tesseract-ocr libtesseract-dev \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=backend /app/backend/target/label-verifier-0.0.1-SNAPSHOT.jar app.jar

# Tesseract needs the language data on disk, not inside the jar.
COPY backend/src/main/resources/tessdata ./tessdata
ENV TESSDATA_PATH=/app/tessdata

# Keep the JVM small enough for a 512 MB container.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC"

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]