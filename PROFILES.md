# Maven Performance Profiles Guide

This document explains the Maven profiles configured for the Federated Cloud Simulation Platform, including when to use each profile, their JVM settings, and troubleshooting tips.

## Overview

The project includes 4 Maven profiles optimized for different scenarios:

| Profile | Purpose | Memory | GC | Logging | Target Machine |
|---------|---------|--------|-----|---------|----------------|
| **dev** (default) | Daily development | 1GB | G1GC | INFO | 8GB RAM workstation |
| **perf** | Performance validation | 4GB | G1GC tuned | WARN | 16GB RAM server |
| **scale** | Large-scale testing | 8GB | ZGC | OFF | 32GB RAM machine |
| **prod** | Production runs | 4GB fixed | G1GC prod | ERROR | 16GB RAM server |

## Profile Details

### Development Profile (dev)

**Purpose:** Quick development testing, CI/CD pipelines

**Configuration:**
- Heap: 512MB min, 1GB max
- GC: G1GC with default settings
- Logging: INFO level (full visibility)
- Timeout: 120 seconds
- Tests: All except `@Tag("scale")`

**Usage:**
```bash
# Explicit activation
mvn test -Pdev

# Default (automatically active)
mvn test

# Run specific test
mvn test -Dtest=FederatedSmokeTest

# Using helper script
scripts\run-dev.bat
```

**When to use:**
- Writing new code
- Running unit tests
- Quick validation
- CI/CD pipelines

**Environment Requirements:**
- Minimum RAM: 4GB
- Recommended RAM: 8GB
- CPU: 2+ cores

---

### Performance Profile (perf)

**Purpose:** Performance validation, regression testing

**Configuration:**
- Heap: 2GB min, 4GB max
- GC: G1GC with tuning (`-XX:MaxGCPauseMillis=200 -XX:InitiatingHeapOccupancyPercent=45`)
- Logging: WARN level (minimal overhead)
- Timeout: 300 seconds (5 minutes)
- Tests: Medium scale included (`@Tag("scale,medium")`)

**Usage:**
```bash
# Run with performance profile
mvn test -Pperf

# Run specific test class
mvn test -Pperf -Dtest=SmallScaleTest

# Run medium scale tests only
mvn test -Pperf -Dgroups=medium

# Using helper script
scripts\run-perf.bat
```

**When to use:**
- Performance validation
- Regression testing
- Medium-scale simulations (100-500 VMs)
- Pre-release testing

**Environment Requirements:**
- Minimum RAM: 8GB
- Recommended RAM: 16GB
- CPU: 4+ cores

**Logback Config:** `logback-perf.xml`

---

### Scale Testing Profile (scale)

**Purpose:** Large-scale stress testing, scalability validation

**Configuration:**
- Heap: 4GB min, 8GB max
- GC: ZGC (low-latency, optimized for large heaps)
- Logging: OFF (completely disabled for maximum performance)
- Timeout: 600 seconds (10 minutes)
- Tests: Scale tests only (`@Tag("scale")`)

**Usage:**
```bash
# Run scale tests
mvn test -Pscale -Dgroups=scale

# Run specific scale test
mvn test -Pscale -Dtest=MediumScaleTest

# Custom heap size
mvn test -Pscale -DargLine.append="-Xmx16g"

# Using helper script (with memory check)
scripts\run-scale.bat
```

**When to use:**
- Large-scale testing (1000+ VMs)
- Stress testing
- Scalability validation
- Performance benchmarking

**Environment Requirements:**
- Minimum RAM: 16GB
- Recommended RAM: 32GB
- CPU: 8+ cores

**Logback Config:** `logback-scale.xml`

**Important Notes:**
- Logging is completely disabled for maximum throughput
- Helper script checks available memory before running
- Tests may take several minutes to complete
- Monitor system resources during execution

---

### Production Profile (prod)

**Purpose:** Production simulation runs

**Configuration:**
- Heap: 4GB fixed (min = max for predictable performance)
- GC: G1GC with production tuning (`-XX:MaxGCPauseMillis=100 -XX:+UseStringDeduplication`)
- Logging: ERROR level (critical errors only)
- Timeout: 300 seconds
- Monitoring: JMX enabled on port 9010
- GC Logging: Enabled to `target/gc.log`

**Usage:**
```bash
# Run simulation with production profile
mvn exec:java -Pprod

# Run tests with production settings
mvn test -Pprod

# Monitor via JMX (connect to localhost:9010)
jconsole localhost:9010
```

**When to use:**
- Production simulation runs
- Performance monitoring
- GC analysis
- Long-running simulations

**Environment Requirements:**
- Minimum RAM: 8GB
- Recommended RAM: 16GB
- CPU: 4+ cores

**Logback Config:** `logback-prod.xml`

**Additional Features:**
- JMX monitoring enabled (no authentication)
- GC logs written to `target/gc.log`
- File-based error logging to `target/production.log`
- Fixed heap size prevents runtime resizing overhead

---

## JVM Flag Reference

### G1GC Flags (dev, perf, prod)

| Flag | Purpose | Profile |
|------|---------|---------|
| `-XX:+UseG1GC` | Enable G1 garbage collector | All G1 profiles |
| `-XX:MaxGCPauseMillis=200` | Target 200ms pause time | perf |
| `-XX:MaxGCPauseMillis=100` | Target 100ms pause time | prod |
| `-XX:InitiatingHeapOccupancyPercent=45` | Start concurrent GC at 45% heap | perf |
| `-XX:+UseStringDeduplication` | Optimize string memory | prod |

### ZGC Flags (scale)

| Flag | Purpose |
|------|---------|
| `-XX:+UseZGC` | Enable ZGC (Java 17+) |
| `-XX:ZCollectionInterval=30` | Collection interval in seconds |
| `-XX:ZAllocationSpikeTolerance=5` | Spike tolerance factor |

### Diagnostic Flags (all profiles)

| Flag | Purpose |
|------|---------|
| `-XX:+HeapDumpOnOutOfMemoryError` | Create heap dump on OOM |
| `-XX:HeapDumpPath=./target/` | Heap dump location |
| `-Xlog:gc*:file=target/gc.log` | GC logging (prod only) |

---

## Test Execution Examples

### Development Testing
```bash
# Run all non-scale tests (default)
mvn test

# Run smoke test quickly
mvn test -Dtest=FederatedSmokeTest

# Run all unit tests
mvn test -Dtest=*Test

# Clean and test
mvn clean test
```

### Performance Validation
```bash
# Run small scale performance tests
mvn test -Pperf -Dtest=SmallScaleTest

# Run all performance tests
mvn test -Pperf

# Verify with specific test
mvn verify -Pperf -Dtest=MediumScaleTest
```

### Scale Testing
```bash
# Run medium scale tests
mvn test -Pscale -Dtest=MediumScaleTest

# Run all scale tests (excluding @Disabled)
mvn test -Pscale -Dgroups=scale

# Enable large scale test manually
mvn test -Pscale -Dtest=LargeScaleTest -DfailIfNoTests=false
```

### Production Runs
```bash
# Run simulation with production config
mvn exec:java -Pprod -Dexec.mainClass="com.cloudsimulation.Main"

# Run with custom scenario
mvn exec:java -Pprod -Dexec.args="configs/my-scenario.yaml"
```

---

## Profile Selection Guide

| Scenario | Recommended Profile | Command |
|----------|---------------------|---------|
| Writing new code | dev | `mvn test` |
| Quick smoke test | dev | `mvn test -Dtest=FederatedSmokeTest` |
| PR validation | perf | `mvn verify -Pperf` |
| Nightly builds | perf | `mvn clean test -Pperf` |
| Performance regression | perf | `mvn test -Pperf` |
| Medium scale (500 VMs) | scale | `mvn test -Pscale -Dtest=MediumScaleTest` |
| Large scale (1000 VMs) | scale | `mvn test -Pscale -DargLine.append="-Xmx16g"` |
| Production simulation | prod | `mvn exec:java -Pprod` |

---

## Troubleshooting

### Error: "Could not reserve enough space for object heap"

**Cause:** Insufficient available RAM for requested heap size

**Solutions:**
1. Use a smaller profile:
   ```bash
   # Instead of scale (8GB), use perf (4GB)
   mvn test -Pperf
   ```

2. Reduce heap size manually:
   ```bash
   # Override heap.max property
   mvn test -Pscale -Dheap.max=4g
   ```

3. Close memory-intensive applications
4. Upgrade machine RAM

---

### Error: "GC overhead limit exceeded"

**Cause:** Application spending >98% time in GC, recovering <2% heap

**Solutions:**
1. Increase heap size:
   ```bash
   mvn test -Pperf -DargLine.append="-Xmx6g"
   ```

2. Switch to ZGC for better large-heap performance:
   ```bash
   mvn test -Pscale
   ```

3. Optimize test to use fewer VMs/cloudlets

---

### Error: "Test timeout"

**Cause:** Test exceeded `forkedProcessTimeoutInSeconds`

**Solutions:**
1. Use profile with higher timeout:
   ```bash
   # scale profile has 600s timeout
   mvn test -Pscale -Dtest=MediumScaleTest
   ```

2. Override timeout property:
   ```bash
   mvn test -Dtest.timeout=900
   ```

3. Check if logging overhead is slowing tests (use perf or scale profile)

---

### Error: "Profile not activated"

**Cause:** Incorrect profile name or syntax

**Solutions:**
1. Verify profile name:
   ```bash
   mvn help:all-profiles
   ```

2. Check active profiles:
   ```bash
   mvn help:active-profiles
   ```

3. Use correct -P syntax:
   ```bash
   # Correct
   mvn test -Pperf

   # Incorrect
   mvn test -P=perf
   mvn test --profile perf
   ```

---

### Issue: Tests slower than expected

**Possible Causes & Solutions:**

1. **Logging Overhead**
   - Switch to perf profile (WARN level)
   - Or scale profile (OFF level)

2. **GC Pauses**
   - Check GC logs: `mvn test -Pprod` (writes to target/gc.log)
   - Consider ZGC: `mvn test -Pscale`

3. **Insufficient Heap**
   - Increase heap: `-DargLine.append="-Xmx8g"`
   - Check memory: Windows Task Manager

4. **Background Processes**
   - Close unnecessary applications
   - Disable antivirus scanning on project directory

---

### Issue: Out of Memory (OOM)

**When OOM Occurs:**
1. Heap dump created automatically at `./target/java_pid*.hprof`
2. Analyze with tools: VisualVM, Eclipse MAT, or JProfiler

**Solutions:**
1. Increase heap size
2. Reduce test workload (fewer VMs/cloudlets)
3. Check for memory leaks (use MemoryOptimizedCloudSim cleanup)

---

## Verification Commands

### Check JVM Settings
```bash
# Show actual JVM arguments
mvn test -Pperf -Dtest=FederatedSmokeTest -X | findstr argLine

# Show active profiles
mvn help:active-profiles

# List all profiles
mvn help:all-profiles
```

### Monitor Test Execution
```bash
# Run with verbose output
mvn test -Pperf -X

# Show only test results
mvn test -Pperf -B

# Generate detailed test report
mvn surefire-report:report -Pperf
```

---

## Performance Tuning Tips

### For Development (dev profile)
- Use `-Dtest=SpecificTest` to run only what you're working on
- Keep heap small (1GB) for faster GC
- INFO logging helps with debugging

### For Performance Testing (perf profile)
- Run on dedicated machine when possible
- Close background applications
- Use consistent hardware for regression testing
- Benchmark with WARN logging (minimal overhead)

### For Scale Testing (scale profile)
- Ensure 16GB+ free RAM before starting
- Disable logging completely (already done in profile)
- Use SSD for any disk I/O
- Monitor system resources during test
- Consider using tmux/screen for long-running tests

### For Production (prod profile)
- Monitor JMX metrics during simulation
- Review GC logs for tuning opportunities
- Keep ERROR logging only
- Use fixed heap size (4GB) for predictable performance

---

## Helper Scripts

The `scripts/` directory contains Windows batch files for common operations:

| Script | Profile | Purpose |
|--------|---------|---------|
| `run-dev.bat` | dev | Quick development testing |
| `run-perf.bat` | perf | Performance validation (checks available RAM) |
| `run-scale.bat` | scale | Scale testing (requires 16GB+ free RAM) |

**Features:**
- Automatic memory availability checks
- User-friendly output
- Exit code handling
- Pass-through arguments

**Usage:**
```bash
# Run with default settings
scripts\run-dev.bat

# Pass additional Maven arguments
scripts\run-perf.bat -Dtest=SmallScaleTest

# Run specific test with scale profile
scripts\run-scale.bat -Dtest=MediumScaleTest
```

---

## Additional Resources

- **Maven Profiles Documentation:** https://maven.apache.org/guides/introduction/introduction-to-profiles.html
- **G1GC Tuning Guide:** https://www.oracle.com/technical-resources/articles/java/g1gc.html
- **ZGC Documentation:** https://wiki.openjdk.org/display/zgc/Main
- **Surefire Plugin:** https://maven.apache.org/surefire/maven-surefire-plugin/

---

## Change Log

| Date | Version | Description |
|------|---------|-------------|
| 2025-11-16 | 1.0 | Initial profiles documentation (Story 2.5) |
