package com.cloudsimulation.benchmarks.benchmarks;

import com.cloudsimulation.benchmarks.core.BenchmarkConfig;
import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import com.cloudsimulation.benchmarks.core.BenchmarkRunner;
import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.core.MemoryOptimizedCloudSim;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.metrics.OptimizedHierarchicalCollector;
import com.cloudsimulation.workload.WorkloadGenerator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.vms.Vm;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark measuring VM creation and cloudlet execution throughput.
 * Measures the rate at which VMs can be created and cloudlets can be processed.
 */
public class ThroughputBenchmark implements BenchmarkRunner {

    private static final int TARGET_VMS = 100;
    private static final int CLOUDLETS_PER_VM = 2;
    private static final int FEDERATIONS = 2;
    private static final int DCS_PER_FED = 2;
    private static final int HOSTS_PER_DC = 13;

    private CloudSimPlus simulation;
    private List<Federation> federations;
    private CloudSimIntegration cloudSim;
    private MemoryOptimizedCloudSim memoryOptimizedCloudSim;
    private OptimizedHierarchicalCollector metricsCollector;

    @Override
    public String getName() {
        return "throughput";
    }

    @Override
    public String getDescription() {
        return "Measures VM creation rate and cloudlet processing throughput";
    }

    @Override
    public void setup() throws Exception {
        simulation = new CloudSimPlus();
        federations = new ArrayList<>();

        // Create infrastructure
        for (int i = 0; i < FEDERATIONS; i++) {
            FederationBuilder builder = new FederationBuilder(simulation);
            Federation federation = builder
                    .withDatacenters(DCS_PER_FED)
                    .withHostsPerDatacenter(HOSTS_PER_DC)
                    .withPesPerHost(4)
                    .withVmsPerHost(0)
                    .build();
            federations.add(federation);
        }

        // Initialize CloudSim
        cloudSim = new CloudSimIntegration();
        memoryOptimizedCloudSim = new MemoryOptimizedCloudSim();
        memoryOptimizedCloudSim.setCleanupEnabled(true);
        metricsCollector = new OptimizedHierarchicalCollector();

        cloudSim.initialize(simulation, federations);
    }

    /**
     * Warmup phase is skipped for CloudSim benchmarks.
     * CloudSim Plus simulations can only be started once per instance and cannot be reset or rerun.
     * This benchmark processes 100 VMs and 200 cloudlets in a single run, providing statistically
     * significant throughput measurements without requiring warmup iterations.
     */
    @Override
    public void warmup(BenchmarkConfig config) {
        // Warmup not applicable - simulation cannot be rerun
    }

    @Override
    public BenchmarkResult run(BenchmarkConfig config) throws Exception {
        long startTime = System.nanoTime();

        // Phase 1: VM Creation
        long vmStartTime = System.nanoTime();
        int vmsPerFederation = TARGET_VMS / FEDERATIONS;
        int totalVmsCreated = 0;

        for (int i = 0; i < FEDERATIONS; i++) {
            FederationBuilder builder = new FederationBuilder(simulation);
            List<Vm> vms = builder.getVmFactory().createVms(vmsPerFederation);
            cloudSim.submitVms(vms);
            totalVmsCreated += vms.size();
        }

        // Invalidate metrics cache after VM creation
        metricsCollector.detectAndInvalidateOnStateChange(cloudSim);

        long vmEndTime = System.nanoTime();
        double vmCreationMs = (vmEndTime - vmStartTime) / 1_000_000.0;
        double vmsPerSecond = (totalVmsCreated / vmCreationMs) * 1000.0;

        // Phase 2: Cloudlet Generation
        long cloudletGenStart = System.nanoTime();
        WorkloadGenerator workloadGenerator = new WorkloadGenerator();
        int cloudletCount = totalVmsCreated * CLOUDLETS_PER_VM;
        List<Cloudlet> cloudlets = workloadGenerator.generateSteadyWorkload(
                CLOUDLETS_PER_VM, cloudletCount / CLOUDLETS_PER_VM);
        long cloudletGenEnd = System.nanoTime();
        double cloudletGenMs = (cloudletGenEnd - cloudletGenStart) / 1_000_000.0;

        // Phase 3: Cloudlet Submission
        long cloudletSubmitStart = System.nanoTime();
        cloudSim.submitCloudlets(cloudlets);
        long cloudletSubmitEnd = System.nanoTime();
        double cloudletSubmitMs = (cloudletSubmitEnd - cloudletSubmitStart) / 1_000_000.0;

        // Phase 4: Simulation Execution
        long simStart = System.nanoTime();
        cloudSim.start();
        long simEnd = System.nanoTime();
        double simMs = (simEnd - simStart) / 1_000_000.0;

        long endTime = System.nanoTime();
        double totalMs = (endTime - startTime) / 1_000_000.0;

        // Calculate throughput metrics
        double cloudletsPerSecond = (cloudlets.size() / simMs) * 1000.0;
        double overallThroughput = (totalVmsCreated / totalMs) * 1000.0;

        // Collect CPU metrics and cache statistics (with error handling)
        double avgCpuUtilization = 0.0;
        double cacheHitRatio = 0.0;
        long cacheRequests = 0;
        long cacheHits = 0;
        long cacheMisses = 0;

        try {
            avgCpuUtilization = metricsCollector.getCachedAvgCpuUtilization(cloudSim);
            OptimizedHierarchicalCollector.CacheStatistics cacheStats = metricsCollector.getCacheStatistics();
            cacheHitRatio = metricsCollector.getCacheHitRatio();
            cacheRequests = cacheStats.getTotalRequests();
            cacheHits = cacheStats.getHits();
            cacheMisses = cacheStats.getMisses();
        } catch (Exception e) {
            // Metrics collection failed - continue with zeros
            System.err.println("Warning: Metrics collection failed: " + e.getMessage());
        }

        // Build result
        BenchmarkResult.Builder resultBuilder = new BenchmarkResult.Builder(getName())
                .profile(config.getProfile())
                .addMetric("total_time_ms", totalMs)
                .addMetric("vm_creation_ms", vmCreationMs)
                .addMetric("cloudlet_generation_ms", cloudletGenMs)
                .addMetric("cloudlet_submission_ms", cloudletSubmitMs)
                .addMetric("simulation_execution_ms", simMs)
                .addMetric("vms_per_second", vmsPerSecond)
                .addMetric("cloudlets_per_second", cloudletsPerSecond)
                .addMetric("overall_throughput_vms_per_sec", overallThroughput)
                .addMetric("avg_cpu_utilization", avgCpuUtilization)
                .addMetric("metrics_cache_hit_ratio", cacheHitRatio)
                .addMetric("metrics_cache_requests", cacheRequests)
                .addMetric("metrics_cache_hits", cacheHits)
                .addMetric("metrics_cache_misses", cacheMisses)
                .addMetadata("total_vms", String.valueOf(totalVmsCreated))
                .addMetadata("total_cloudlets", String.valueOf(cloudlets.size()));

        // Check if throughput meets minimum threshold (>10 VMs/second)
        if (overallThroughput < 10.0) {
            resultBuilder.failureReason(
                    String.format("Overall throughput %.2f VMs/s below threshold of 10 VMs/s",
                            overallThroughput));
        }

        return resultBuilder.build();
    }

    @Override
    public void teardown() throws Exception {
        if (cloudSim != null) {
            memoryOptimizedCloudSim.forceCleanup();
            cloudSim.terminate();
        }
        federations.clear();
        simulation = null;
        cloudSim = null;
        memoryOptimizedCloudSim = null;
        metricsCollector = null;
    }
}
