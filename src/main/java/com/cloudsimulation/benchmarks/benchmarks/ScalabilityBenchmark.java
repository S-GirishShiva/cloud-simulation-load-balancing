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
import java.util.List;

/**
 * Benchmark measuring scalability and linear scaling characteristics.
 * Tests performance at different VM counts (25, 50, 100, 200) and calculates scaling factor.
 */
public class ScalabilityBenchmark implements BenchmarkRunner {

    private static final int[] VM_SCALES = {25, 50, 100, 200};
    private static final int CLOUDLETS_PER_VM = 2;

    private CloudSimPlus simulation;
    private List<Federation> federations;
    private CloudSimIntegration cloudSim;
    private MemoryOptimizedCloudSim memoryOptimizedCloudSim;

    @Override
    public String getName() {
        return "scalability";
    }

    @Override
    public String getDescription() {
        return "Measures linear scaling characteristics across different VM counts";
    }

    @Override
    public void setup() throws Exception {
        // Setup will be done per scale iteration
    }

    /**
     * Warmup phase is skipped for CloudSim benchmarks.
     * CloudSim Plus simulations can only be started once per instance and cannot be reset or rerun.
     * This benchmark executes 4 separate simulation runs at different scales (25, 50, 100, 200 VMs),
     * providing comprehensive scalability analysis across multiple data points in a single measurement cycle.
     */
    @Override
    public void warmup(BenchmarkConfig config) {
        // Warmup not applicable - multiple simulations run at different scales
    }

    @Override
    public BenchmarkResult run(BenchmarkConfig config) throws Exception {
        double[] executionTimes = new double[VM_SCALES.length];
        double[] throughputs = new double[VM_SCALES.length];

        // Run benchmark at each scale
        for (int i = 0; i < VM_SCALES.length; i++) {
            int vmCount = VM_SCALES[i];

            // Setup fresh simulation for each scale
            setupSimulation();

            long startTime = System.nanoTime();

            // Create and submit VMs
            FederationBuilder builder = new FederationBuilder(simulation);
            List<Vm> vms = builder.getVmFactory().createVms(vmCount);
            cloudSim.submitVms(vms);

            // Create and submit cloudlets
            WorkloadGenerator workloadGenerator = new WorkloadGenerator();
            List<Cloudlet> cloudlets = workloadGenerator.generateSteadyWorkload(
                    CLOUDLETS_PER_VM, vmCount);
            cloudSim.submitCloudlets(cloudlets);

            // Run simulation
            cloudSim.start();

            long endTime = System.nanoTime();

            double executionMs = (endTime - startTime) / 1_000_000.0;
            executionTimes[i] = executionMs;
            throughputs[i] = (vmCount / executionMs) * 1000.0;  // VMs per second

            // Cleanup for next iteration
            teardownSimulation();
        }

        // Calculate scaling metrics
        ScalingMetrics metrics = calculateScalingMetrics(executionTimes, throughputs);

        // Build result
        BenchmarkResult.Builder resultBuilder = new BenchmarkResult.Builder(getName())
                .profile(config.getProfile())
                .addMetric("scale_25_vms_time_ms", executionTimes[0])
                .addMetric("scale_50_vms_time_ms", executionTimes[1])
                .addMetric("scale_100_vms_time_ms", executionTimes[2])
                .addMetric("scale_200_vms_time_ms", executionTimes[3])
                .addMetric("scale_25_vms_throughput", throughputs[0])
                .addMetric("scale_50_vms_throughput", throughputs[1])
                .addMetric("scale_100_vms_throughput", throughputs[2])
                .addMetric("scale_200_vms_throughput", throughputs[3])
                .addMetric("scaling_factor", metrics.scalingFactor)
                .addMetric("scaling_efficiency_percent", metrics.scalingEfficiency)
                .addMetric("throughput_degradation_percent", metrics.throughputDegradation)
                .addMetadata("vm_scales", "25,50,100,200")
                .addMetadata("cloudlets_per_vm", String.valueOf(CLOUDLETS_PER_VM));

        // Check if scaling factor is acceptable (>0.85 means good linear scaling)
        if (metrics.scalingFactor < 0.85) {
            resultBuilder.failureReason(
                    String.format("Poor scaling factor %.3f (threshold: 0.85)", metrics.scalingFactor));
        }

        return resultBuilder.build();
    }

    @Override
    public void teardown() throws Exception {
        // Final cleanup
        teardownSimulation();
    }

    /**
     * Setup a fresh simulation instance
     */
    private void setupSimulation() throws Exception {
        simulation = new CloudSimPlus();
        federations = new ArrayList<>();

        // Create infrastructure with enough capacity for largest scale
        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(20)  // Enough for 200 VMs
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
     * Teardown current simulation instance
     */
    private void teardownSimulation() throws Exception {
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

        System.gc();
        Thread.sleep(50);  // Brief pause for GC
    }

    /**
     * Calculate scaling metrics from execution times and throughputs
     */
    private ScalingMetrics calculateScalingMetrics(double[] executionTimes, double[] throughputs) {
        // Ideal linear scaling: 2x VMs should take ~2x time (constant throughput)
        // Scaling factor = actual_time_ratio / expected_time_ratio

        // Compare 100 VMs vs 25 VMs (4x increase)
        double expectedRatio = 4.0;  // 100 / 25
        double actualRatio = executionTimes[2] / executionTimes[0];  // time_100 / time_25
        double scalingFactor = expectedRatio / actualRatio;

        // Scaling efficiency (1.0 = perfect linear scaling)
        double scalingEfficiency = scalingFactor * 100.0;

        // Throughput degradation (how much throughput drops from smallest to largest scale)
        double throughputDegradation = ((throughputs[0] - throughputs[3]) / throughputs[0]) * 100.0;

        return new ScalingMetrics(scalingFactor, scalingEfficiency, throughputDegradation);
    }

    /**
     * Holder for scaling metrics
     */
    private static class ScalingMetrics {
        final double scalingFactor;         // Closer to 1.0 is better (linear)
        final double scalingEfficiency;     // Percentage (100% = perfect)
        final double throughputDegradation;  // Percentage drop in throughput

        ScalingMetrics(double scalingFactor, double scalingEfficiency, double throughputDegradation) {
            this.scalingFactor = scalingFactor;
            this.scalingEfficiency = scalingEfficiency;
            this.throughputDegradation = throughputDegradation;
        }
    }
}
