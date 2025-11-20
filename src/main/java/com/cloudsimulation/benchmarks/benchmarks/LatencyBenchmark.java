package com.cloudsimulation.benchmarks.benchmarks;

import com.cloudsimulation.benchmarks.core.BenchmarkConfig;
import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import com.cloudsimulation.benchmarks.core.BenchmarkRunner;
import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.core.MemoryOptimizedCloudSim;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.workload.WorkloadGenerator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Benchmark measuring operation latency and response times.
 * Tracks percentiles (p50, p95, p99) for VM creation and cloudlet submission.
 */
public class LatencyBenchmark implements BenchmarkRunner {

    private static final int SAMPLES = 100;  // Number of operations to measure
    private static final int WARMUP_SAMPLES = 10;

    private CloudSimPlus simulation;
    private List<Federation> federations;
    private CloudSimIntegration cloudSim;
    private MemoryOptimizedCloudSim memoryOptimizedCloudSim;

    @Override
    public String getName() {
        return "latency";
    }

    @Override
    public String getDescription() {
        return "Measures operation latency percentiles (p50, p95, p99) for VM and cloudlet operations";
    }

    @Override
    public void setup() throws Exception {
        simulation = new CloudSimPlus();
        federations = new ArrayList<>();

        // Create infrastructure
        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(10)
                .withPesPerHost(4)
                .withVmsPerHost(0)
                .build();
        federations.add(federation);

        cloudSim = new CloudSimIntegration();
        memoryOptimizedCloudSim = new MemoryOptimizedCloudSim();
        memoryOptimizedCloudSim.setCleanupEnabled(true);

        cloudSim.initialize(simulation, federations);
    }

    /**
     * Warmup phase is skipped for CloudSim benchmarks.
     * CloudSim Plus simulations can only be started once per instance and cannot be reset or rerun.
     * This benchmark includes an internal warmup of 10 samples before measuring 100 operation
     * latencies, providing statistically valid percentile calculations without external warmup.
     */
    @Override
    public void warmup(BenchmarkConfig config) {
        // Warmup not applicable - simulation has internal warmup phase
    }

    @Override
    public BenchmarkResult run(BenchmarkConfig config) throws Exception {
        // Warmup phase
        for (int i = 0; i < WARMUP_SAMPLES; i++) {
            FederationBuilder builder = new FederationBuilder(simulation);
            List<Vm> warmupVms = builder.getVmFactory().createVms(1);
            cloudSim.submitVms(warmupVms);
        }

        // Measure VM creation latency
        long[] vmLatencies = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long startTime = System.nanoTime();

            FederationBuilder builder = new FederationBuilder(simulation);
            List<Vm> vms = builder.getVmFactory().createVms(1);
            cloudSim.submitVms(vms);

            long endTime = System.nanoTime();
            vmLatencies[i] = endTime - startTime;
        }

        // Measure cloudlet submission latency
        WorkloadGenerator workloadGenerator = new WorkloadGenerator();
        long[] cloudletLatencies = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long startTime = System.nanoTime();

            List<Cloudlet> cloudlets = workloadGenerator.generateSteadyWorkload(1, 1);
            cloudSim.submitCloudlets(cloudlets);

            long endTime = System.nanoTime();
            cloudletLatencies[i] = endTime - startTime;
        }

        // Calculate percentiles
        LatencyStats vmStats = calculateLatencyStats(vmLatencies);
        LatencyStats cloudletStats = calculateLatencyStats(cloudletLatencies);

        // Build result
        BenchmarkResult.Builder resultBuilder = new BenchmarkResult.Builder(getName())
                .profile(config.getProfile())
                .addMetric("vm_creation_p50_us", vmStats.p50)
                .addMetric("vm_creation_p95_us", vmStats.p95)
                .addMetric("vm_creation_p99_us", vmStats.p99)
                .addMetric("vm_creation_min_us", vmStats.min)
                .addMetric("vm_creation_max_us", vmStats.max)
                .addMetric("vm_creation_avg_us", vmStats.avg)
                .addMetric("cloudlet_submission_p50_us", cloudletStats.p50)
                .addMetric("cloudlet_submission_p95_us", cloudletStats.p95)
                .addMetric("cloudlet_submission_p99_us", cloudletStats.p99)
                .addMetric("cloudlet_submission_min_us", cloudletStats.min)
                .addMetric("cloudlet_submission_max_us", cloudletStats.max)
                .addMetric("cloudlet_submission_avg_us", cloudletStats.avg)
                .addMetadata("samples", String.valueOf(SAMPLES));

        // Check if p95 latencies are acceptable (< 1ms)
        if (vmStats.p95 > 1000 || cloudletStats.p95 > 1000) {
            resultBuilder.failureReason(
                    String.format("p95 latency exceeds 1ms threshold: VM=%.2f us, Cloudlet=%.2f us",
                            vmStats.p95, cloudletStats.p95));
        }

        return resultBuilder.build();
    }

    @Override
    public void teardown() throws Exception {
        if (cloudSim != null) {
            memoryOptimizedCloudSim.forceCleanup();
            cloudSim.terminate();
        }
        if (federations != null) {
            federations.clear();
        }
        simulation = null;
        cloudSim = null;
        memoryOptimizedCloudSim = null;
    }

    /**
     * Calculate latency statistics from nanosecond measurements
     * @param latenciesNs Array of latencies in nanoseconds
     * @return LatencyStats with percentiles in microseconds
     */
    private LatencyStats calculateLatencyStats(long[] latenciesNs) {
        // Convert to microseconds and sort
        double[] latenciesUs = new double[latenciesNs.length];
        for (int i = 0; i < latenciesNs.length; i++) {
            latenciesUs[i] = latenciesNs[i] / 1000.0;  // Convert ns to us
        }
        Arrays.sort(latenciesUs);

        // Calculate percentiles
        double p50 = percentile(latenciesUs, 50);
        double p95 = percentile(latenciesUs, 95);
        double p99 = percentile(latenciesUs, 99);
        double min = latenciesUs[0];
        double max = latenciesUs[latenciesUs.length - 1];

        // Calculate average
        double sum = 0;
        for (double latency : latenciesUs) {
            sum += latency;
        }
        double avg = sum / latenciesUs.length;

        return new LatencyStats(p50, p95, p99, min, max, avg);
    }

    /**
     * Calculate percentile from sorted array
     */
    private double percentile(double[] sortedValues, int percentile) {
        if (sortedValues.length == 0) {
            return 0;
        }

        int index = (int) Math.ceil((percentile / 100.0) * sortedValues.length) - 1;
        index = Math.max(0, Math.min(index, sortedValues.length - 1));

        return sortedValues[index];
    }

    /**
     * Holder for latency statistics (all values in microseconds)
     */
    private static class LatencyStats {
        final double p50;
        final double p95;
        final double p99;
        final double min;
        final double max;
        final double avg;

        LatencyStats(double p50, double p95, double p99, double min, double max, double avg) {
            this.p50 = p50;
            this.p95 = p95;
            this.p99 = p99;
            this.min = min;
            this.max = max;
            this.avg = avg;
        }
    }
}
