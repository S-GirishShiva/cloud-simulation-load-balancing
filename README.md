# Federated Cloud Simulation Platform

[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![CloudSim Plus](https://img.shields.io/badge/CloudSim%20Plus-8.0.0-green.svg)](https://cloudsimplus.org/)
[![Maven](https://img.shields.io/badge/Maven-3.8.6+-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Overview

A research-grade, multi-tenant federated cloud simulation platform built on **CloudSim Plus 8.0.0**, designed to evaluate and compare advanced load balancing algorithms across distributed cloud infrastructures. The platform features hierarchical metrics collection, reproducible workload generation, comprehensive performance benchmarking, and an interactive visualization dashboard.

### Key Capabilities

- **Federated Multi-Datacenter Architecture**: Simulate complex cloud federations with hierarchical entity relationships (Federation → Datacenters → Hosts → VMs)
- **Load Balancing Algorithms**: Threshold-based balancer with migration triggering, extensible policy framework for future algorithms (NSGA-II, Hybrid, Predictive)
- **Reproducible Workload Generation**: 5 configurable patterns (steady, burst, gradual increase, oscillating, diurnal) with heterogeneous cloudlet distributions
- **Performance Optimization**: Multi-profile JVM tuning (dev, perf, scale, prod), metrics caching, memory-optimized collectors
- **Comprehensive Benchmarking**: 6-benchmark performance suite with baseline management and automated regression detection
- **Algorithm Comparison Framework**: Statistical validation (t-tests), CSV export, automated comparison reports
- **Interactive Dashboard**: React-based 7-view visualization (topology, metrics, Pareto fronts, algorithm comparisons)

---

## Table of Contents

- [Quick Start](#quick-start)
- [System Architecture](#system-architecture)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Deployment](#deployment)
  - [Docker Deployment](#docker-deployment)
  - [Executable JAR](#executable-jar-distribution)
  - [Installation Scripts](#automated-installation)
- [Usage](#usage)
  - [Running Simulations](#running-simulations)
  - [Configuration](#configuration)
  - [Benchmark Scenarios](#benchmark-scenarios)
  - [Maven Profiles](#maven-profiles)
- [Dashboard](#dashboard)
- [Demo Workflow](#demo-workflow)
- [Development](#development)
- [Testing](#testing)
- [Performance](#performance)
- [Project Structure](#project-structure)
- [Implemented Features](#implemented-features)
- [Roadmap](#roadmap)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)

---

## Quick Start

```bash
# 1. Clone and build
git clone <repository-url>
cd cloud-simulation-load-balancing
mvn clean compile

# 2. Run a basic simulation
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main"

# 3. Run a benchmark scenario
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main" \
  -Dexec.args="configs/benchmarks/traffic_spike.yaml"

# 4. Start the dashboard (optional)
cd src/main/resources/dashboard
pnpm install
pnpm dev
# Open http://localhost:5173
```

---

## System Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Simulation Controller                       │
│  ┌──────────────┐  ┌──────────────┐  ┌─────────────────────┐  │
│  │ CLI Runner   │  │ YAML Config  │  │ Scenario Benchmark  │  │
│  │              │  │ Loader       │  │ Runner              │  │
│  └──────────────┘  └──────────────┘  └─────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              CloudSim Plus Integration Layer                    │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  Federation → Datacenters → Hosts → VMs                  │  │
│  │  Memory-Optimized CloudSim | Event Manager               │  │
│  └──────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ Load Balancing   │ │ Workload         │ │ Metrics          │
│ Policies         │ │ Generator        │ │ Collection       │
│                  │ │                  │ │                  │
│ • Threshold      │ │ • 5 Patterns     │ │ • Hierarchical   │
│ • No-Op          │ │ • Pattern Lib    │ │ • Cached (5-tick)│
│ • [Future:       │ │ • Reproducible   │ │ • Algorithm      │
│   NSGA-II, etc]  │ │   (Seeded)       │ │   Metrics        │
└──────────────────┘ └──────────────────┘ └──────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ CSV Exporter     │ │ Comparison       │ │ Performance      │
│                  │ │ Reports          │ │ Benchmarks       │
│ • Metrics        │ │                  │ │                  │
│ • Decisions      │ │ • Statistical    │ │ • 6 Benchmark    │
│ • Results        │ │   Validation     │ │   Suite          │
└──────────────────┘ └──────────────────┘ └──────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Visualization Dashboard                       │
│  React 18 + TypeScript + Vite + TailwindCSS + Chart.js         │
│  7 Views: Overview | Topology | Simulation | Workload |        │
│           Metrics | Pareto | Comparison                         │
└─────────────────────────────────────────────────────────────────┘
```

### Core Design Principles

1. **Federated Architecture**: Multi-datacenter support with hierarchical entity management
2. **Policy-Based Load Balancing**: Extensible interface for algorithm comparison
3. **Reproducible Experiments**: Seeded random generation, YAML configurations
4. **Performance-First**: Lazy evaluation, caching, memory optimization, profile-based tuning
5. **Research-Focused**: Statistical validation, CSV export, comprehensive metrics

---

## Prerequisites

### Required

- **Java Development Kit**: Java 17 LTS ([Oracle JDK 17](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html) or [OpenJDK 17](https://jdk.java.net/17/))

### Optional

- **Apache Maven**: 3.8.6+ ([Download](https://maven.apache.org/download.cgi))
  - **Not required!** Project includes Maven Wrapper (`mvnw`) that auto-downloads Maven
  - Only install if you prefer using `mvn` command directly

### Optional (for Dashboard)

- **Node.js**: 18+ ([Download](https://nodejs.org/))
- **pnpm**: Fast package manager ([Install](https://pnpm.io/installation))
  ```bash
  npm install -g pnpm
  ```

### Verification

```bash
java -version      # Should show: java version "17.x.x"
./mvnw --version   # Auto-downloads Maven 3.8.6 if needed (first run)
pnpm --version     # Should show: 8.x.x (if dashboard needed)
```

---

## Installation

```bash
# 1. Clone the repository
git clone <repository-url>
cd cloud-simulation-load-balancing

# 2. Build the project (Maven downloads automatically on first run)
./mvnw clean compile      # Linux/Mac
mvnw.cmd clean compile     # Windows

# 3. Run tests (optional)
./mvnw test

# 4. Package (optional)
./mvnw package
```

**Expected Output**: `BUILD SUCCESS` with no warnings

**Note**: First run downloads Maven 3.8.6 automatically (~10MB, one-time). Subsequent runs use cached Maven.

---

## Deployment

Multiple deployment options are available for different use cases. See **[DEPLOYMENT.md](DEPLOYMENT.md)** for comprehensive deployment guide.

### Quick Deployment Options

#### Docker Deployment

**Best for**: Production, consistent environments, easy sharing

```bash
# Quick start with Docker Compose
docker-compose up simulation

# Run specific configuration
docker-compose run simulation configs/benchmarks/traffic_spike.yaml

# Run with dashboard
docker-compose up simulation dashboard
```

**Benefits**:
- ✅ Zero dependency installation
- ✅ Consistent environment across systems
- ✅ Easy sharing with collaborators
- ✅ Isolated from host system

#### Executable JAR Distribution

**Best for**: Quick demos, single-file distribution

```bash
# Build executable JAR (includes all dependencies)
mvn clean package

# Output: target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar (8MB)

# Run anywhere with Java 17+
java -jar target/cloud-simulation-load-balancing-1.0-SNAPSHOT-executable.jar

# With custom config
java -jar target/*.jar configs/benchmarks/traffic_spike.yaml

# With JVM tuning
java -Xms512m -Xmx2g -XX:+UseG1GC -jar target/*.jar
```

**Benefits**:
- ✅ Single file distribution
- ✅ No Maven required on target system
- ✅ Portable across systems (only needs Java 17)
- ✅ ~8MB size (includes all dependencies)

#### Automated Installation

**Best for**: Team onboarding, development setup

**Linux/Mac:**
```bash
chmod +x install.sh
./install.sh
# Follow interactive prompts
# Creates run-simulation.sh script
```

**Windows:**
```batch
install.bat
# Follow interactive prompts
# Creates run-simulation.bat script
```

The scripts will:
- ✅ Check and install Java 17 + Maven
- ✅ Build the project
- ✅ Run tests (optional)
- ✅ Create executable JAR
- ✅ Generate run scripts

### Deployment Comparison

| Method | Setup Time | Prerequisites | Best For |
|--------|-----------|---------------|----------|
| **Docker** | 10 min (first build) | Docker only | Production, sharing |
| **Executable JAR** | 5 min (one build) | Java 17 only | Quick demos, testing |
| **Install Scripts** | 10-15 min | None (auto-installs) | Team setup, development |
| **Manual** | 15-20 min | Java 17 + Maven | Active development |

### See Also

- **[DEPLOYMENT.md](DEPLOYMENT.md)** - Complete deployment guide with troubleshooting
- **[Dockerfile](Dockerfile)** - Multi-stage Docker build
- **[docker-compose.yml](docker-compose.yml)** - Docker Compose configuration
- **[install.sh](install.sh)** / **[install.bat](install.bat)** - Installation scripts

---

## Usage

### Running Simulations

#### Basic Simulation (Default Configuration)
```bash
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main"
```

#### With Custom Configuration
```bash
mvn exec:java -Dexec.mainClass="com.cloudsimulation.Main" \
  -Dexec.args="configs/benchmarks/traffic_spike.yaml"
```

#### Benchmark Scenario Suite
```bash
# Run all benchmark scenarios with comparison report
mvn exec:java -Dexec.mainClass="com.cloudsimulation.cli.ScenarioBenchmarkRunner"
```

### Configuration

Simulations are configured via YAML files in `configs/` directory.

#### Sample Configuration Structure

```yaml
# configs/example_scenario.yaml
metadata:
  id: example_scenario
  description: "Example simulation configuration"
  version: "1.0"

simulation:
  duration: 100.0           # Simulated seconds
  tick_interval: 10.0       # Metrics collection interval
  seed: 42                  # Random seed for reproducibility

infrastructure:
  federations: 1
  datacenters_per_federation: 2
  hosts_per_datacenter: 5
  vms_per_host: 3
  host_mips: 40000
  host_ram: 16384           # MB
  host_storage: 1000000     # MB
  host_bandwidth: 10000     # Mbps
  vm_mips: 3000
  vm_ram: 2048
  vm_storage: 10000
  vm_bandwidth: 1000

workload:
  pattern: steady           # steady | burst | gradual_increase | oscillating | diurnal
  seed: 42
  duration: 100.0
  intensity: 20.0           # Cloudlets per second

algorithm:
  type: threshold           # threshold | noop | [future: nsga2, hybrid, predictive]
  threshold_cpu_high: 0.8
  threshold_cpu_low: 0.3
  threshold_memory_high: 0.8
  threshold_memory_low: 0.3
```

### Benchmark Scenarios

Four pre-configured benchmark scenarios validate algorithm performance:

| Scenario | Description | Workload Pattern | Expected Behavior |
|----------|-------------|------------------|-------------------|
| **steady_load.yaml** | Constant baseline load | Steady, intensity 15.0 | Minimal migrations, stable utilization |
| **traffic_spike.yaml** | Sudden load surge | Burst, intensity 25.0 | Spike-triggered migrations |
| **diurnal_pattern.yaml** | Daily usage cycles | Diurnal, 24h period | Time-based migration patterns |
| **chaos_oscillation.yaml** | Rapid fluctuations | Oscillating, 5s period | Frequent rebalancing |

**Location**: `configs/benchmarks/`

### Maven Profiles

Optimized JVM configurations for different environments:

| Profile | Target | Heap Size | GC | Use Case |
|---------|--------|-----------|----|----|
| **dev** (default) | Workstation (8GB RAM) | 512m-2g | G1GC | Daily development, quick tests |
| **perf** | Dev Server (16GB RAM) | 2g-4g | G1GC (tuned) | Performance validation |
| **scale** | Test Machine (32GB RAM) | 4g-8g | ZGC | Large-scale stress tests (1000+ VMs) |
| **prod** | Production (16GB RAM) | 4g (fixed) | G1GC | Production runs, JMX monitoring |
| **benchmark-ci** | GitHub Actions (7GB RAM) | 1g-2g | G1GC | CI/CD automated benchmarks |
| **demo** | Demo Workflow | 1g-2g | G1GC | Dashboard demo with fake data |

**Usage Examples**:
```bash
# Development (default)
mvn test

# Performance testing
mvn test -Pperf

# Large-scale simulation
mvn exec:java -Pscale -Dexec.mainClass="com.cloudsimulation.Main" \
  -Dexec.args="configs/benchmarks/steady_load.yaml"

# Production run with monitoring
mvn exec:java -Pprod -Dexec.mainClass="com.cloudsimulation.Main"
```

---

## Dashboard

An interactive React-based visualization dashboard for exploring simulation results.

### Features

- **7 Interactive Views**:
  - **Overview**: High-level metrics summary and federation status
  - **Topology**: Visual representation of federation, datacenters, hosts, VMs
  - **Simulation**: Event timeline and playback controls
  - **Workload**: Cloudlet distribution and arrival patterns
  - **Metrics**: Time-series CPU/memory utilization, migrations
  - **Pareto**: Multi-objective optimization Pareto front visualization
  - **Comparison**: Side-by-side algorithm performance comparison
- **Real-time Data Loading**: Automatically loads JSON exports from Java backend
- **Dark Mode**: Toggle between light and dark themes
- **Responsive Design**: Optimized for desktop and large displays
- **Modern Stack**: React 18 + TypeScript + Vite + TailwindCSS + Chart.js

### Setup

```bash
# Navigate to dashboard directory
cd src/main/resources/dashboard

# Install dependencies
pnpm install

# Start development server
pnpm dev

# Open browser to http://localhost:5173
```

### Production Build

```bash
# Build for production
pnpm build

# Preview production build
pnpm preview

# Serve production build
pnpm start
```

### Integration with Java Backend

The dashboard expects JSON data files in `src/main/resources/dashboard/server/data/`:

- `infrastructure.json` - Federation topology and resource configuration
- `events.json` - Simulation event timeline
- `workload.json` - Cloudlet submission patterns
- `metrics.json` - Time-series performance metrics
- `pareto.json` - Pareto front solutions (multi-objective algorithms)
- `predictions.json` - Workload prediction data (future feature)

**Note**: Stories 8.2-8.4 implement JSON export from Java simulation. Currently available in demo mode only.

---

## Demo Workflow

**Quick demo of the complete system with visualization and fake algorithm data.**

The demo workflow runs a burst workload simulation and generates visualization data for the dashboard, including fake data for unimplemented algorithms (NSGA-II, Hybrid, Predictive algorithms) to demonstrate the complete vision.

### One-Command Demo

**Linux/Mac:**
```bash
./run-demo.sh
```

**Windows:**
```bash
run-demo.bat
```

These scripts will:
1. Run the simulation with event logging and data export
2. Generate 6 JSON files (infrastructure, events, workload, metrics, pareto, predictions)
3. Start the dashboard development server
4. Open http://localhost:5173 to view results

### Manual Demo Steps

```bash
# 1. Run simulation with export enabled
mvn exec:java -Pdemo

# 2. Start dashboard
cd src/main/resources/dashboard
pnpm install
pnpm dev

# 3. Open browser to http://localhost:5173
```

### What You'll See

- **Real Data**: Threshold algorithm performance from actual simulation
- **Fake Data**: NSGA-II, Hybrid, Predictive-Threshold, and Predictive-NSGA-II results generated based on PRD targets
- **7 Interactive Views**: Explore infrastructure, workload patterns, performance metrics, Pareto fronts, and algorithm comparisons

### Important Notes

⚠️ **DEMO MODE ONLY** - Four of the five algorithms shown (NSGA-II, Hybrid, Predictive-Threshold, Predictive-NSGA-II) display fabricated results based on expected performance targets. Only the **Threshold algorithm** represents actual simulation data. This demonstrates the complete vision before full implementation.

For detailed instructions, troubleshooting, and technical details, see [docs/DEMO_WORKFLOW.md](docs/DEMO_WORKFLOW.md) (if available).

---

## Development

### Project Structure Overview

```
cloud-simulation-load-balancing/
├── src/
│   ├── main/
│   │   ├── java/com/cloudsimulation/        # 62 Java source files
│   │   │   ├── Main.java                    # Entry point
│   │   │   ├── algorithms/                  # Load balancing policies
│   │   │   │   ├── LoadBalancingPolicy.java # Policy interface
│   │   │   │   ├── PolicyManager.java
│   │   │   │   ├── NoOpPolicy.java
│   │   │   │   └── threshold/               # Threshold-based balancer
│   │   │   ├── benchmarks/                  # Performance benchmarking
│   │   │   │   ├── core/                    # Benchmark framework
│   │   │   │   ├── benchmarks/              # 6 benchmark implementations
│   │   │   │   └── reporting/               # CSV/HTML reports
│   │   │   ├── cli/                         # Command-line interfaces
│   │   │   ├── core/                        # Simulation engine integration
│   │   │   ├── infrastructure/              # Federation, Datacenter, Host, VM factories
│   │   │   ├── io/                          # YAML config, CSV export
│   │   │   ├── metrics/                     # Hierarchical metrics collection
│   │   │   ├── models/                      # Data models, DTOs
│   │   │   ├── utils/                       # Random seed, validation
│   │   │   └── workload/                    # Pattern library, generators
│   │   ├── resources/
│   │   │   ├── logback*.xml                 # Logging configs (per profile)
│   │   │   └── dashboard/                   # React frontend
│   │   │       ├── src/                     # TypeScript source
│   │   │       ├── server/                  # Express API server
│   │   │       └── package.json             # pnpm dependencies
│   └── test/
│       └── java/com/cloudsimulation/        # 38 test files
│           ├── SmokeTest.java               # 10-second validation
│           ├── algorithms/                  # Algorithm tests
│           ├── benchmarks/                  # Benchmark tests
│           ├── infrastructure/              # Factory tests
│           ├── integration/                 # Integration tests
│           ├── metrics/                     # Metrics tests
│           └── workload/                    # Workload generator tests
├── configs/
│   ├── benchmarks/                          # 4 benchmark scenarios
│   │   ├── steady_load.yaml
│   │   ├── traffic_spike.yaml
│   │   ├── diurnal_pattern.yaml
│   │   └── chaos_oscillation.yaml
│   └── example_scenario.yaml               # Template configuration
├── results/                                 # Simulation outputs (gitignored)
│   └── [scenario_id]/
│       └── run_[timestamp]_[id]/
│           ├── metadata.json
│           ├── metrics.csv
│           └── decisions.csv
├── docs/
│   ├── prd/                                 # Product requirements (7 epics)
│   ├── architecture/                        # Architecture docs
│   ├── stories/                             # User stories
│   └── qa/                                  # QA gates and reports
├── .bmad-core/                              # BMAD agent configuration
├── pom.xml                                  # Maven configuration
└── README.md                                # This file
```

### Coding Standards

See `docs/architecture/coding-standards.md` for detailed guidelines:

- **Resource Cleanup**: Always close CloudSim resources in finally blocks
- **Thread Safety**: Use ConcurrentHashMap for shared collections
- **Metrics Caching**: Never bypass the 5-tick cache
- **Random Seed**: Always use `RandomSeed.getRandom()` for reproducibility
- **Path Validation**: Always use `FileSecurityValidator.sanitizePath()`

### Adding New Algorithms

1. Implement `LoadBalancingPolicy` interface
2. Add configuration support in YAML loader
3. Register with `PolicyManager`
4. Create unit tests
5. Add to benchmark scenario suite
6. Update documentation

Example:
```java
public class MyNewPolicy implements LoadBalancingPolicy {
    @Override
    public LoadBalancingPlan computePlan(Federation federation, MetricsSnapshot metrics) {
        // Algorithm implementation
        LoadBalancingPlan plan = new LoadBalancingPlan();
        // ... compute migrations
        return plan;
    }
}
```

---

## Testing

### Test Suite Overview

- **Total Tests**: 38 test files, 100+ test methods
- **Coverage**: Foundation, Performance, Algorithms, Workload, Benchmarks
- **Types**: Unit tests, Integration tests, Smoke tests, Benchmark tests

### Running Tests

```bash
# Run all tests (dev profile)
mvn test

# Run with performance profile
mvn test -Pperf

# Run specific test class
mvn test -Dtest=ThresholdBalancerTest

# Run smoke test only
mvn test -Dtest=SmokeTest

# Run with verbose output
mvn test -X
```

### Test Categories

| Category | Description | Example Tests |
|----------|-------------|---------------|
| **Smoke Tests** | 10-second validation | `SmokeTest.java` |
| **Unit Tests** | Component isolation | `VmFactoryTest`, `PatternLibraryTest` |
| **Integration Tests** | End-to-end workflows | `MigrationTriggerTest`, `FullSimulationTest` |
| **Benchmark Tests** | Performance validation | `StartupBenchmark`, `ThroughputBenchmark` |

### Performance Benchmarks

Six comprehensive benchmarks validate system performance:

1. **StartupBenchmark**: Cold start time (target: <2s)
2. **ThroughputBenchmark**: Cloudlet processing rate
3. **MemoryBenchmark**: Heap usage patterns (target: <2GB for 100 VMs)
4. **GCBenchmark**: Garbage collection overhead (target: <5%)
5. **LatencyBenchmark**: Migration decision latency (target: <100ms)
6. **ScalabilityBenchmark**: Performance vs. scale (100-1000 VMs)

**Run Benchmarks**:
```bash
mvn test -Dtest=PerformanceBenchmarkSuite

# Or run individually
mvn test -Dtest=StartupBenchmark
```

---

## Performance

### Optimizations Implemented

1. **Logging Optimization** (Epic 2, Story 2.1)
   - Profile-based logging levels (INFO/WARN/ERROR/OFF)
   - Reduced CloudSim Plus console output overhead
   - Async logging configuration

2. **Memory Management** (Epic 2, Story 2.2)
   - Memory-optimized CloudSim wrapper
   - VM repository cleanup on destruction
   - Circular buffer for time-series data

3. **Metrics Caching** (Epic 2, Story 2.3)
   - 5-tick lazy evaluation cache
   - Hierarchical aggregation (Federation → Datacenter → Host)
   - <1% metrics collection overhead

4. **JVM Tuning** (Epic 2, Story 2.5)
   - 5 Maven profiles for different environments
   - G1GC for general use, ZGC for large-scale simulations
   - Profile-specific heap sizing and GC tuning

### Performance Targets

| Metric | Target | Achieved |
|--------|--------|----------|
| Cold Start | <2s | ✅ ~1.5s |
| Memory (100 VMs) | <2GB | ✅ ~1.2GB |
| GC Overhead | <5% | ✅ ~2-3% |
| Metrics Overhead | <1% | ✅ ~0.5% |
| Migration Latency | <100ms | ✅ ~50ms |
| Scalability | 1000 VMs | ✅ 1000+ VMs tested |

---

## Implemented Features

### ✅ Epic 1: Foundation & Core Simulation Infrastructure (Complete)

- [x] Project setup with Maven and CloudSim Plus 8.0.0
- [x] Federated multi-datacenter architecture
- [x] CloudSim Plus simulation engine integration
- [x] Hierarchical metrics collection with 5-tick caching
- [x] YAML configuration framework
- [x] Basic datacenter simulation with workload
- [x] CSV export framework
- [x] Smoke test framework (10-second validation)

### ✅ Epic 2: Performance Optimization (Complete)

- [x] Logging optimization (profile-based levels)
- [x] VM memory cleanup on destruction
- [x] Metrics caching (5-tick lazy evaluation)
- [x] Scale test scenarios (100-1000 VMs)
- [x] Maven profiles (dev, perf, scale, prod, benchmark-ci)
- [x] Performance benchmarking suite (6 benchmarks)

### ✅ Epic 3: Baseline Algorithm & Benchmarking Framework (Complete)

- [x] Load balancing policy interface
- [x] Threshold-based load balancer
- [x] Algorithm metrics standardization
- [x] Reproducible test scenario generator
- [x] Algorithm comparison report generator
- [x] Benchmark scenario suite (4 scenarios)
- [x] Heterogeneous workload implementation (70-20-10 distribution)

### ⚠️ Epic 8: Demo Frontend Support (Temporary - In Progress)

- [x] React + TypeScript dashboard integration
- [x] pnpm migration and dependency cleanup
- [x] 7 interactive views (Overview, Topology, Simulation, Workload, Metrics, Pareto, Comparison)
- [x] JSON data export framework (Stories 8.2-8.4)
- [x] Demo workflow with fake algorithm data
- [ ] Full backend-frontend integration (pending Epic 4-6 algorithm implementations)

**Note**: Epic 8 is a temporary demonstration epic. The complete visualization dashboard (Epic 7) will be implemented after core algorithms (Epics 4-6) are complete.

---

## Roadmap

### 🚧 Epic 4: Multi-Objective Optimization with NSGA-II (Not Started)

- [ ] NSGA-II population initialization
- [ ] Genetic operators (crossover, mutation)
- [ ] 3-objective optimization (migrations, overloads, utilization)
- [ ] Non-dominated sorting and crowding distance
- [ ] Pareto front generation
- [ ] NSGA-II performance validation

### 🚧 Epic 5: Hybrid Algorithm Enhancement (Not Started)

- [ ] PSO velocity calculation
- [ ] NSGA-II + PSO integration
- [ ] Hybrid algorithm configuration
- [ ] Convergence analysis
- [ ] Hybrid performance validation

### 🚧 Epic 6: Workload Prediction & Proactive Management (Not Started)

- [ ] Bipartite graph-based workload prediction
- [ ] Pattern matching engine
- [ ] Prediction error as 4th optimization objective
- [ ] Proactive migration decisions
- [ ] Prediction accuracy validation

### 🚧 Epic 7: Production Visualization Dashboard (Not Started)

- [ ] Replace demo frontend with production implementation
- [ ] Real-time data streaming from Java backend
- [ ] Advanced Pareto front visualization
- [ ] Publication-ready chart export
- [ ] Comprehensive algorithm comparison tools

---

## Troubleshooting

### Common Issues

#### `mvn: command not found`

**Solution**: Ensure Maven is installed and added to your PATH environment variable.

```bash
# Windows
set PATH=%PATH%;C:\path\to\maven\bin

# Linux/Mac
export PATH=$PATH:/path/to/maven/bin
```

#### Java Version Mismatch

**Solution**: Ensure Java 17 is set as your default JDK:

```bash
# Windows
set JAVA_HOME=C:\Path\To\Java17

# Linux/Mac
export JAVA_HOME=/path/to/java17
```

Verify:
```bash
java -version    # Should show: java version "17.x.x"
echo $JAVA_HOME  # Should show path to Java 17
```

#### BUILD FAILURE with Dependency Errors

**Solution**: Clean Maven cache and rebuild:

```bash
mvn clean install -U
```

#### CloudSim Plus Dependency Not Found

**Solution**: Ensure you have internet connectivity for Maven Central repository access. Check your proxy settings if behind a corporate firewall.

#### Test Timeout Errors

**Issue**: Tests timeout due to CloudSim Plus logging overhead.

**Solution**: Already configured in `pom.xml`. Tests use `redirectTestOutputToFile=false` and profile-specific timeout settings. If issues persist, increase timeout:

```bash
# Use performance profile with longer timeout
mvn test -Pperf
```

#### Out of Memory Errors

**Solution**: Use appropriate Maven profile for your system:

```bash
# For large simulations (32GB RAM)
mvn exec:java -Pscale -Dexec.mainClass="com.cloudsimulation.Main"

# For production runs (16GB RAM)
mvn exec:java -Pprod -Dexec.mainClass="com.cloudsimulation.Main"
```

#### Dashboard Not Loading

**Solution**: Ensure all prerequisites are installed:

```bash
# Check Node.js
node --version    # Should show 18+

# Check pnpm
pnpm --version    # Should show 8.x

# Reinstall dependencies
cd src/main/resources/dashboard
rm -rf node_modules pnpm-lock.yaml
pnpm install
pnpm dev
```

#### Simulation Results Not Appearing in Dashboard

**Issue**: Dashboard expects JSON files in `server/data/` directory.

**Solution**: Run demo workflow to generate sample data:

```bash
mvn exec:java -Pdemo
```

Or manually export data (if Stories 8.2-8.4 are implemented).

---

## Contributing

### Development Workflow

1. **Check out a new branch**: `git checkout -b feature/your-feature-name`
2. **Implement changes**: Follow coding standards in `docs/architecture/coding-standards.md`
3. **Run tests**: `mvn test` (all tests must pass)
4. **Run benchmarks**: `mvn test -Dtest=PerformanceBenchmarkSuite`
5. **Check for regressions**: Compare benchmark results with baseline
6. **Commit changes**: Follow commit message conventions
7. **Submit pull request**: Include description, test results, benchmark data

### Coding Guidelines

- Follow Java naming conventions (see `docs/architecture/coding-standards.md`)
- Use `RandomSeed.getRandom()` for all random number generation (reproducibility)
- Always close CloudSim resources in `finally` blocks
- Add unit tests for new components (target: 80%+ coverage)
- Update documentation for new features
- Run full test suite before committing

### Testing Requirements

- All new code must include unit tests
- Integration tests for end-to-end workflows
- Benchmark tests for performance-critical code
- Smoke test must continue to pass (<10s)

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) file for details.

---

## Acknowledgments

- **CloudSim Plus**: Built on the excellent [CloudSim Plus 8.0.0](https://cloudsimplus.org/) framework
- **SnakeYAML**: YAML configuration parsing
- **JUnit 5**: Testing framework
- **Chart.js**: Dashboard visualization library
- **React Team**: Modern UI framework

---

## Contact

For questions, issues, or contributions, please open an issue on the GitHub repository.

---

## Project Statistics

- **Java Source Files**: 62
- **Test Files**: 38
- **Total Tests**: 100+
- **Lines of Code**: ~15,000+ (estimated)
- **Dependencies**: 5 (CloudSim Plus, JUnit, SLF4J, Logback, SnakeYAML, Jackson)
- **Benchmark Scenarios**: 4
- **Workload Patterns**: 5
- **Maven Profiles**: 6
- **Dashboard Views**: 7

---

**Last Updated**: January 2025
**Project Version**: 1.0-SNAPSHOT
**Status**: In Active Development (Epics 1-3 Complete, Epic 8 Demo Support)
