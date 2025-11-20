# Benchmark Scenarios

This directory contains standard benchmark scenarios for validating load balancing algorithm performance across different workload conditions.

## Overview

The benchmark suite consists of 4 carefully designed scenarios that test algorithm behavior under varying load patterns:

1. **steady_load** - Constant baseline load
2. **diurnal_pattern** - Gradual daily cycle simulation
3. **traffic_spike** - Sudden burst stress test
4. **chaos_oscillation** - Rapid fluctuation resilience test

## Scenario Descriptions

### steady_load.yaml

**Purpose**: Tests algorithm performance under predictable, constant load.

**Characteristics**:
- Duration: 300 seconds
- Workload: 8 cloudlets/second (constant rate)
- Pattern: Steady arrival rate
- Seed: 100

**Expected Behavior**:
- Stable CPU utilization around 50-60%
- Minimal VM migrations (algorithm should find stable placement)
- Low overload events (< 5% of time)
- Very low SLA violations (< 1%)

**Validation Checkpoints**:
- **100s**: System should be in stable state
- **200s**: Utilization should remain steady
- **300s**: Final metrics should show consistent performance

**Use Case**: Baseline comparison for algorithm efficiency under optimal conditions.

---

### diurnal_pattern.yaml

**Purpose**: Tests algorithm adaptation to gradual load variations simulating daily usage cycles.

**Characteristics**:
- Duration: 300 seconds
- Workload: 5 cloudlets/second base intensity
- Pattern: Sine wave (frequency 0.01)
- Seed: 200

**Expected Behavior**:
- Oscillating utilization between 30-80%
- Moderate VM migrations as load changes
- Few overload events (algorithms should predict pattern)
- Low SLA violations (< 5%)

**Validation Checkpoints**:
- **75s**: Low load period (~30% utilization)
- **150s**: Peak load period (~80% utilization)
- **225s**: Returning to low load
- **300s**: Stable state after adapting to pattern

**Use Case**: Evaluates algorithm ability to handle predictable but variable load patterns.

---

### traffic_spike.yaml

**Purpose**: Tests algorithm resilience to sudden load spikes.

**Characteristics**:
- Duration: 300 seconds
- Workload: 5 cloudlets/second baseline
- Pattern: Burst (10x spike from 140-160s)
- Seed: 42

**Expected Behavior**:
- Pre-spike (0-140s): Stable 5 cloudlets/second
- Spike window (140-160s): 50 cloudlets/second burst
- Post-spike (160-300s): Return to baseline
- High migration activity during spike
- Possible SLA violations during spike

**Validation Checkpoints**:
- **100s**: Baseline metrics, stable utilization
- **150s**: During spike, expect high migration activity
- **250s**: Post-spike recovery, system stabilization

**Use Case**: Tests algorithm response to unexpected load surges and recovery capability.

---

### chaos_oscillation.yaml

**Purpose**: Stress test for algorithm stability under rapid, unpredictable load fluctuations.

**Characteristics**:
- Duration: 300 seconds
- Workload: 10 cloudlets/second base intensity
- Pattern: High-frequency oscillation (frequency 0.05)
- Seed: 300

**Expected Behavior**:
- Highly volatile utilization (20-90% swings)
- High VM migration count (constant rebalancing)
- Possible overload events (challenging to predict)
- Moderate SLA violations (5-15%) acceptable
- Key metric: Algorithm stability (no crashes)

**Validation Checkpoints**:
- **60s, 120s, 180s, 240s, 300s**: Verify algorithm doesn't crash under volatility

**Use Case**: Validates algorithm robustness and ensures graceful degradation under chaos.

---

## Running Individual Scenarios

You can run a single scenario using the standard simulation command:

```bash
# Run steady load scenario
java -jar target/cloud-simulation.jar configs/benchmarks/steady_load.yaml

# Run diurnal pattern scenario
java -jar target/cloud-simulation.jar configs/benchmarks/diurnal_pattern.yaml

# Run traffic spike scenario
java -jar target/cloud-simulation.jar configs/benchmarks/traffic_spike.yaml

# Run chaos oscillation scenario
java -jar target/cloud-simulation.jar configs/benchmarks/chaos_oscillation.yaml
```

## Running Full Benchmark Suite

Execute all scenarios with one or more algorithms:

```bash
# Run all scenarios with threshold algorithm (default)
java -jar target/cloud-simulation.jar --benchmark-scenarios

# Run all scenarios with specific algorithms
java -jar target/cloud-simulation.jar --benchmark-scenarios --algorithms threshold,nsga2,hybrid

# Specify custom benchmarks directory
java -jar target/cloud-simulation.jar --benchmark-scenarios --benchmarks-dir configs/benchmarks
```

## Interpreting Results

### Master Comparison Matrix

The benchmark suite generates a master comparison matrix showing:

```
Scenario            | threshold                         | nsga2                            |
-----------------------------------------------------------------------------------------------------------------
steady_load         | M:15 O:2 U:55% S:0 ★             | M:20 O:1 U:58% S:0               |
diurnal_pattern     | M:45 O:8 U:60% S:3               | M:38 O:5 U:62% S:2 ★             |
traffic_spike       | M:120 O:25 U:75% S:12            | M:95 O:18 U:72% S:8 ★            |
chaos_oscillation   | M:250 O:50 U:65% S:35            | ✗ TIMEOUT                        |
```

**Legend**:
- **M**: Total VM migrations
- **O**: Overload events
- **U**: Average CPU utilization (%)
- **S**: SLA violations
- **★**: Pareto-optimal algorithm for this scenario
- **✗**: Failed execution (CRASH, TIMEOUT, YAML_PARSE_ERROR)

### Pareto Optimality

Algorithms marked with ★ are Pareto-optimal, meaning they are not dominated by any other algorithm across all metrics (migrations, overloads, utilization, SLA violations).

## Interpreting Master Comparison Matrix Output

The benchmark suite generates a comprehensive comparison across all scenarios and algorithms.

### Reading the Matrix

Each cell in the matrix shows:
- **M**: Total migrations (lower is better)
- **O**: Overload events (lower is better)
- **U**: Average utilization (higher is better, target 50-80%)
- **S**: SLA violations (lower is better)
- **★**: Pareto-optimal (best trade-off across metrics)
- **✗**: Failed (timeout or crash)

### Example Analysis

```
steady_load: M:15 O:2 U:55% S:0 ★
```

This means:
- 15 total VM migrations occurred
- 2 overload events detected
- 55% average CPU utilization (good balance)
- 0 SLA violations (excellent)
- ★ indicates this algorithm is Pareto-optimal for this scenario

### Comparing Algorithms

- **Lower migrations**: Algorithm finds stable placements faster
- **Lower overloads**: Better load prediction/distribution
- **Higher utilization** (50-80% range): Efficient resource usage
- **Lower SLA violations**: Better quality of service

## Troubleshooting

### Timeout Errors

If a scenario times out (exceeds 60 seconds):
- Check infrastructure configuration (may be too large for 60s limit)
- Verify workload intensity isn't excessive
- Review algorithm complexity (may need optimization)

### YAML Parse Errors

If YAML fails to load:
- Validate YAML syntax (use online YAML validator)
- Check all required fields are present: `scenarioId`, `duration`, `seed`, `infrastructureConfig`, `workloadConfig`, `policyConfig`
- Verify numeric values are not quoted (except `scenarioId`)

### High Failure Rates

If many scenarios fail:
- Check logs for exception stack traces
- Verify CloudSim Plus version compatibility
- Ensure adequate system resources (memory, CPU)
- Review algorithm implementation for bugs

### Unexpected Metrics

If metrics don't match expected behavior:
- Verify seed value matches scenario specification
- Check workload pattern configuration (patternType, intensity, frequency)
- Review infrastructure configuration (may need scaling)
- Validate algorithm policy is correctly registered

## Adding Custom Scenarios

To create a new benchmark scenario:

1. Create a new YAML file in `configs/benchmarks/`
2. Follow the template structure (see existing scenarios)
3. Document expected behavior in comments
4. Define validation checkpoints
5. Choose an appropriate seed for reproducibility
6. Test individually before adding to suite

**Required fields**:
```yaml
scenarioId: "my_scenario"
duration: 300
seed: <unique-int>
infrastructureConfig:
  federationCount: 1
  datacentersPerFederation: 2
  hostsPerDatacenter: 10
  vmsPerHost: 5
workloadConfig:
  patternType: "steady|burst|gradual_increase|oscillating|diurnal"
  seed: <same-as-scenario-seed>
  duration: 300
  intensity: <cloudlets-per-second>
policyConfig:
  defaultPolicy: "threshold"
  enableMetrics: true
```

## Related Documentation

- **Architecture**: See `docs/architecture/source-tree.md` for project structure
- **Workload Patterns**: See `WorkloadGenerator` class for pattern details
- **Algorithm Metrics**: See `AlgorithmMetrics` model for tracked metrics
- **Comparison Reports**: See `ComparisonReportGenerator` for report generation logic

## Contact & Support

For questions or issues with benchmark scenarios, please file an issue in the project repository.
