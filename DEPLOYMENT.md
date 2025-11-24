# Deployment Guide

This guide covers all deployment options for the Federated Cloud Simulation Platform, from quick Docker deployment to manual installation on various systems.

## Table of Contents

- [Quick Comparison](#quick-comparison)
- [Option 1: Docker (Recommended)](#option-1-docker-recommended)
- [Option 2: Automated Installation Scripts](#option-2-automated-installation-scripts)
- [Option 3: Executable JAR](#option-3-executable-jar)
- [Option 4: Manual Installation](#option-4-manual-installation)
- [Deployment Scenarios](#deployment-scenarios)
- [Troubleshooting](#troubleshooting)

---

## Quick Comparison

| Method | Pros | Cons | Best For |
|--------|------|------|----------|
| **Docker** | Zero dependencies, consistent environment, easy sharing | Requires Docker installation | Production, sharing with others |
| **Automated Scripts** | Native installation, guided setup | Platform-specific | Team onboarding |
| **Executable JAR** | Single file, portable, no build needed | Requires Java 17 | Quick demos, testing |
| **Manual** | Full control, development-friendly | More steps, requires all dependencies | Active development |

---

## Option 1: Docker (Recommended)

**Best for**: Production deployment, consistent environments, sharing with collaborators

### Prerequisites
- Docker 20.10+ ([Install Docker](https://docs.docker.com/get-docker/))
- Docker Compose 2.0+ (usually included with Docker Desktop)

### Quick Start

#### 1. Run Simulation with Docker

```bash
# Build and run simulation with default config
docker-compose up simulation

# Run with specific configuration
docker-compose run simulation configs/benchmarks/steady_load.yaml

# Run in background
docker-compose up -d simulation
```

#### 2. Run with Dashboard

```bash
# Start both simulation and dashboard
docker-compose up simulation dashboard

# Access dashboard at http://localhost:5173
```

#### 3. Run Full Benchmark Suite

```bash
# Run all benchmark scenarios
docker-compose --profile benchmark up benchmark
```

### Docker Commands Reference

```bash
# Build images
docker-compose build

# View logs
docker-compose logs -f simulation

# Stop services
docker-compose down

# Remove volumes (clean results)
docker-compose down -v

# Run one-off simulation
docker run --rm -v $(pwd)/results:/app/results cloud-simulation:latest \
  configs/benchmarks/traffic_spike.yaml
```

### Docker Image Details

- **Base Image**: Eclipse Temurin 17 JRE Alpine (~180MB)
- **Final Image Size**: ~300MB (includes CloudSim Plus + dependencies)
- **Build Time**: ~5-10 minutes (first build)
- **Memory**: Configure via JAVA_OPTS environment variable

### Custom Configuration

Create a `docker-compose.override.yml` for local customization:

```yaml
version: '3.8'
services:
  simulation:
    environment:
      - JAVA_OPTS=-Xms2g -Xmx4g -XX:+UseG1GC
    volumes:
      - ./my-configs:/app/my-configs
```

---

## Option 2: Automated Installation Scripts

**Best for**: Team onboarding, setting up development environments

### Linux/Mac

```bash
# Make script executable
chmod +x install.sh

# Run installation
./install.sh

# Follow interactive prompts
```

The script will:
1. Check for Java 17 and Maven
2. Offer to install missing prerequisites
3. Build the project
4. Run tests (optional)
5. Create executable JAR
6. Create run scripts

### Windows

```batch
# Run installation script
install.bat

# Follow interactive prompts
```

### What Gets Installed

**Required:**
- Java 17 JDK (if missing)
- Apache Maven 3.8.6+ (if missing)

**Optional:**
- Node.js 18+ (for dashboard)
- pnpm (for dashboard)

### Post-Installation

After successful installation:

```bash
# Linux/Mac
./run-simulation.sh                                    # Default simulation
./run-simulation.sh configs/benchmarks/traffic_spike.yaml  # Custom config

# Windows
run-simulation.bat                                     # Default simulation
run-simulation.bat configs\benchmarks\traffic_spike.yaml   # Custom config
```

---

## Option 3: Executable JAR

**Best for**: Quick demos, testing, sharing with users who have Java installed

### Prerequisites
- Java 17 JRE or JDK ([Download](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html))

### Build Executable JAR

If you have Maven installed:

```bash
# Build JAR with all dependencies
mvn clean package

# Output: target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar
```

### Run Executable JAR

```bash
# Basic run (default configuration)
java -jar target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar

# With custom configuration
java -jar target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar \
  configs/benchmarks/traffic_spike.yaml

# With JVM tuning (recommended)
java -Xms512m -Xmx2g -XX:+UseG1GC \
  -jar target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar \
  configs/benchmarks/steady_load.yaml
```

### Profile-Specific JARs

```bash
# Development (2GB heap)
java -Xms512m -Xmx2g -XX:+UseG1GC -jar app.jar

# Performance (4GB heap)
java -Xms2g -Xmx4g -XX:+UseG1GC -jar app.jar

# Large-scale (8GB heap, ZGC)
java -Xms4g -Xmx8g -XX:+UseZGC -jar app.jar

# Production (4GB fixed)
java -Xms4g -Xmx4g -XX:+UseG1GC -jar app.jar
```

### Distributing the JAR

The executable JAR is self-contained and can be shared as a single file:

1. Build: `mvn package`
2. Share: `target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar`
3. Include: `configs/` directory for configurations
4. Run: `java -jar <jar-file> [config.yaml]`

**JAR Size**: ~40-50MB (includes CloudSim Plus and all dependencies)

---

## Option 4: Manual Installation

**Best for**: Active development, customization

See [README.md - Installation](README.md#installation) section for detailed manual installation instructions.

Quick summary:
```bash
# 1. Install Java 17
# 2. Install Maven 3.8.6+
# 3. Clone repository
# 4. Build project
mvn clean compile

# 5. Run simulation
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

---

## Deployment Scenarios

### Scenario 1: Research Collaboration

**Goal**: Share simulation with collaborators who need to reproduce results

**Recommended**: Docker

```bash
# 1. Provide Dockerfile and docker-compose.yml
# 2. Collaborators run:
docker-compose up simulation

# 3. Results appear in shared results/ directory
```

### Scenario 2: CI/CD Pipeline

**Goal**: Automated testing and benchmarking

**Recommended**: Docker + Executable JAR

```yaml
# .github/workflows/benchmark.yml
jobs:
  benchmark:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - name: Run benchmarks
        run: mvn test -Pbenchmark-ci
```

### Scenario 3: Conference Demo

**Goal**: Quick demo on unknown machine

**Recommended**: Executable JAR

```bash
# 1. Copy JAR and configs/ to USB drive
# 2. On demo machine (with Java 17):
java -jar cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar \
  configs/benchmarks/traffic_spike.yaml

# 3. Show results
```

### Scenario 4: Production Research Server

**Goal**: Long-running simulations on dedicated server

**Recommended**: Docker with production profile

```yaml
# docker-compose.prod.yml
services:
  simulation:
    image: cloud-simulation:latest
    environment:
      - JAVA_OPTS=-Xms4g -Xmx4g -XX:+UseG1GC
    volumes:
      - /data/simulations/results:/app/results
    restart: unless-stopped
```

### Scenario 5: Teaching Lab

**Goal**: Students run simulations on lab machines

**Recommended**: Automated Installation Scripts

```bash
# 1. Run on each lab machine:
./install.sh

# 2. Students use:
./run-simulation.sh configs/benchmarks/steady_load.yaml
```

---

## Troubleshooting

### Docker Issues

#### Docker Build Fails

**Problem**: Network errors during dependency download

**Solution**:
```bash
# Use cached layers, increase timeout
docker-compose build --no-cache
```

#### Container Exits Immediately

**Problem**: No configuration provided or invalid config

**Solution**:
```bash
# Check logs
docker-compose logs simulation

# Verify config exists
docker-compose run simulation ls configs/benchmarks/
```

#### Permission Denied on Results Directory

**Problem**: Docker user doesn't have write access

**Solution**:
```bash
# Fix permissions
chmod -R 777 results/

# Or run with host user
docker-compose run --user $(id -u):$(id -g) simulation
```

### Installation Script Issues

#### Java Installation Fails

**Problem**: Package manager doesn't have Java 17

**Solution**: Download and install manually:
- Oracle JDK: https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html
- OpenJDK: https://jdk.java.net/17/

#### Maven Not in PATH

**Problem**: Maven installed but command not found

**Solution**:
```bash
# Linux/Mac
export PATH=$PATH:/path/to/maven/bin
echo 'export PATH=$PATH:/path/to/maven/bin' >> ~/.bashrc

# Windows
set PATH=%PATH%;C:\path\to\maven\bin
# Or use System Environment Variables GUI
```

### Executable JAR Issues

#### OutOfMemoryError

**Problem**: Insufficient heap space for simulation

**Solution**: Increase heap size
```bash
java -Xms2g -Xmx4g -jar app.jar
```

#### ClassNotFoundException

**Problem**: Shaded JAR missing dependencies

**Solution**: Rebuild with Maven Shade plugin
```bash
mvn clean package
```

#### Configuration File Not Found

**Problem**: Relative path from wrong directory

**Solution**: Use absolute path or run from project root
```bash
cd /path/to/project
java -jar target/app-executable.jar configs/example.yaml
```

### General Issues

#### Simulation Hangs/Slow Performance

**Solutions**:
1. Use appropriate Maven profile for your system
2. Reduce simulation scale (fewer VMs)
3. Enable performance logging profile
4. Check GC overhead: `java -Xlog:gc app.jar`

#### Results Not Generated

**Solutions**:
1. Check results/ directory exists
2. Verify write permissions
3. Check logs for errors
4. Ensure simulation completed (check exit code)

---

## Performance Tuning

### Memory Configuration

| Simulation Scale | Recommended Heap | JVM Flags |
|------------------|------------------|-----------|
| Small (10-50 VMs) | 512MB-1GB | `-Xms512m -Xmx1g` |
| Medium (50-200 VMs) | 1GB-2GB | `-Xms1g -Xmx2g` |
| Large (200-500 VMs) | 2GB-4GB | `-Xms2g -Xmx4g` |
| Extra Large (500-1000+ VMs) | 4GB-8GB | `-Xms4g -Xmx8g -XX:+UseZGC` |

### GC Selection

- **G1GC**: Default, good for most workloads
- **ZGC**: Better for large heaps (>4GB), low latency
- **ParallelGC**: Maximum throughput, higher pause times

---

## Support

For deployment issues:
1. Check [README.md Troubleshooting](README.md#troubleshooting)
2. Review logs: `mvn exec:java -X` or `docker logs`
3. Open issue on GitHub with deployment method and error logs

---

**Last Updated**: January 2025
