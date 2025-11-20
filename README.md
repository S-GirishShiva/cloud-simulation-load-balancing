# Federated Cloud Simulation Platform

## Overview

A multi-tenant federated cloud simulation platform built on CloudSim Plus 8.0.0, designed to evaluate advanced load balancing algorithms across distributed cloud infrastructures. This research tool enables simulation of complex cloud environments with configurable workloads, resource scheduling, and performance metrics collection.

## Prerequisites

### Java Development Kit
- **Version:** Java 17 LTS
- **Download:** [Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [OpenJDK 17](https://jdk.java.net/17/)
- **Verify installation:**
  ```bash
  java -version
  ```
  Expected output: `java version "17.x.x"`

### Apache Maven
- **Version:** Maven 3.8.6 or higher
- **Download:** [Apache Maven](https://maven.apache.org/download.cgi)
- **Verify installation:**
  ```bash
  mvn --version
  ```
  Expected output: `Apache Maven 3.8.6` or higher

## Build Instructions

### Clean and Compile
```bash
mvn clean compile
```

Expected output: `BUILD SUCCESS` with no warnings

### Run Basic Simulation
```bash
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

Expected output:
```
CloudSim Plus initialized successfully!
CloudSim Plus version: 8.0.0
Simulation completed.
```

## Project Structure

```
cloud-simulation-load-balancing/
├── src/
│   ├── main/
│   │   ├── java/              # Java source files
│   │   └── resources/          # Configuration files
│   └── test/
│       └── java/              # Test files
├── configs/                   # Simulation configuration files
├── results/                   # Simulation output (gitignored)
├── pom.xml                    # Maven project configuration
└── README.md                  # This file
```

## Troubleshooting

### Issue: `mvn: command not found`
**Solution:** Ensure Maven is installed and added to your PATH environment variable.

### Issue: Java version mismatch
**Solution:** Ensure Java 17 is set as your default JDK:
```bash
# Windows
set JAVA_HOME=C:\Path\To\Java17
# Linux/Mac
export JAVA_HOME=/path/to/java17
```

### Issue: BUILD FAILURE with dependency errors
**Solution:** Clean Maven cache and rebuild:
```bash
mvn clean install -U
```

### Issue: CloudSim Plus dependency not found
**Solution:** Ensure you have internet connectivity for Maven Central repository access.

## Dependencies

- **CloudSim Plus 8.0.0** - Cloud simulation framework
- **JUnit 5.9.0** - Testing framework
- **SLF4J 1.7.36** - Logging facade
- **Logback 1.4.5** - Logging implementation

## Development

This project follows standard Maven conventions. Source code is located in `src/main/java` with the base package `com.cloudsimulation`.

### Running Tests
```bash
mvn test
```

### Generating JAR
```bash
mvn package
```
