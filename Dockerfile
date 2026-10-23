# ==============================================================================
# NEWSROOM Cloud Edition - Multi-Stage Container Dockerfile
# Optimized for Render, Fly.io, Railway, and Docker Cloud Environments
# ==============================================================================

# --- Stage 1: Build Container ---
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy dependency libraries and sources
COPY lib ./lib
COPY src ./src

# Compile all Java sources and copy web resources into bin
RUN mkdir -p bin && \
    find src/main/java -name "*.java" > sources.txt && \
    javac -cp "lib/*" -d bin -encoding UTF-8 @sources.txt && \
    if [ -d "src/main/resources" ]; then cp -r src/main/resources/* bin/ 2>/dev/null || true; fi

# --- Stage 2: Lightweight Runtime Container ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy compiled classes and dependencies from builder
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
COPY newsroom.properties.example ./newsroom.properties

# Render binds dynamically via $PORT
ENV PORT=10000
ENV JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"

# Expose Render default port
EXPOSE 10000

# Launch NEWSROOM Web Service
CMD ["java", "-Djava.awt.headless=true", "-cp", "bin:lib/*", "com.newsroom.NewsReaderApplication", "--web"]
