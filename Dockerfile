# Multi-stage Dockerfile for Federated Cloud Simulation Platform
# Stage 1: Build Java application
FROM maven:3.8.6-eclipse-temurin-17 AS java-builder

WORKDIR /app

# Copy Maven configuration
COPY pom.xml .

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
COPY configs ./configs

# Build application
RUN mvn clean package -DskipTests

# Stage 2: Build Dashboard (optional)
FROM node:18-alpine AS dashboard-builder

WORKDIR /dashboard

# Copy dashboard files
COPY src/main/resources/dashboard/package.json ./
COPY src/main/resources/dashboard/pnpm-lock.yaml ./

# Install pnpm
RUN npm install -g pnpm

# Install dependencies
RUN pnpm install --frozen-lockfile

# Copy dashboard source
COPY src/main/resources/dashboard ./

# Build dashboard
RUN pnpm build

# Stage 3: Runtime image
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install bash for scripts
RUN apk add --no-cache bash

# Copy built JAR from java-builder
COPY --from=java-builder /app/target/*.jar ./app.jar

# Copy configurations
COPY --from=java-builder /app/configs ./configs

# Copy dashboard build (optional)
COPY --from=dashboard-builder /dashboard/dist ./dashboard

# Create results directory
RUN mkdir -p results

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx2g -XX:+UseG1GC"

# Expose ports (dashboard if needed)
EXPOSE 5173 3000

# Default command: run simulation
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar \"$@\"", "--"]

# Default: run with example config
CMD ["configs/benchmarks/traffic_spike.yaml"]
