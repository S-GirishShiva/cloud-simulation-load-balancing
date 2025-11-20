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

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark measuring garbage collection impact on simulation performance.
 * Tracks GC pause times, frequency, and overhead during VM/cloudlet operations.
 */
public class GCBenchmark implements BenchmarkRunner {

    private static final int TARGET_VMS = 150;
    private static final int CLOUDLETS_PER_VM = 3;

    private CloudSimPlus simulation;
    private List<Federation> federations;
    private CloudSimIntegration cloudSim;
    private MemoryOptimizedCloudSim memoryOptimizedCloudSim;

    private List<GarbageCollectorMXBean> gcBeans;

    @Override
    public String getName() {
        return "gc";
    }

    @Override
    public String getDescription() {
        return "Measures garbage collection pause times, frequency, and overhead";
    }

    @Override
    public void setup() throws Exception {
        // Get all GC beans for monitoring
        gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // Force GC to get clean baseline
        System.gc();
        Thread.sleep(100);
    }

    /**
     * Warmup phase is skipped for CloudSim benchmarks.
     * CloudSim Plus simulations can only be started once per instance and cannot be reset or rerun.
     * GC metrics are collected across 4 simulation phases (infrastructure, VMs, cloudlets, execution),
     * providing detailed garbage collection profiling in a single run.
     */
    @Override
    public void warmup(BenchmarkConfig config) {
        // Warmup not applicable - simulation cannot be rerun
    }

    @Override
    public BenchmarkResult run(BenchmarkConfig config) throws Exception {
        // Record baseline GC stats
        GCStats baseline = captureGCStats();

        // Phase 1: Infrastructure setup
        simulation = new CloudSimPlus();
        federations = new ArrayList<>();

        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(15)
                .withPesPerHost(4)
                .withVmsPerHost(0)
                .build();
        federations.add(federation);

        cloudSim = new CloudSimIntegration();
        memoryOptimizedCloudSim = new MemoryOptimizedCloudSim();
        memoryOptimizedCloudSim.setCleanupEnabled(true);

        cloudSim.initialize(simulation, List.of(federation));

        GCStats afterInfrastructure = captureGCStats();

        // Phase 2: VM Creation (GC intensive)
        long vmStartTime = System.nanoTime();
        builder = new FederationBuilder(simulation);
        List<Vm> vms = builder.getVmFactory().createVms(TARGET_VMS);
        cloudSim.submitVms(vms);
        long vmEndTime = System.nanoTime();

        GCStats afterVMs = captureGCStats();

        // Phase 3: Cloudlet Creation
        WorkloadGenerator workloadGenerator = new WorkloadGenerator();
        List<Cloudlet> cloudlets = workloadGenerator.generateSteadyWorkload(
                CLOUDLETS_PER_VM, TARGET_VMS);
        cloudSim.submitCloudlets(cloudlets);

        GCStats afterCloudlets = captureGCStats();

        // Phase 4: Simulation Execution
        long simStartTime = System.nanoTime();
        cloudSim.start();
        long simEndTime = System.nanoTime();

        GCStats afterSimulation = captureGCStats();

        // Calculate GC metrics
        long infrastructureCollections = afterInfrastructure.totalCollections - baseline.totalCollections;
        long vmCollections = afterVMs.totalCollections - afterInfrastructure.totalCollections;
        long cloudletCollections = afterCloudlets.totalCollections - afterVMs.totalCollections;
        long simulationCollections = afterSimulation.totalCollections - afterCloudlets.totalCollections;

        long infrastructurePauseMs = afterInfrastructure.totalPauseTime - baseline.totalPauseTime;
        long vmPauseMs = afterVMs.totalPauseTime - afterInfrastructure.totalPauseTime;
        long cloudletPauseMs = afterCloudlets.totalPauseTime - afterVMs.totalPauseTime;
        long simulationPauseMs = afterSimulation.totalPauseTime - afterCloudlets.totalPauseTime;

        long totalCollections = afterSimulation.totalCollections - baseline.totalCollections;
        long totalPauseMs = afterSimulation.totalPauseTime - baseline.totalPauseTime;

        double vmTimeMs = (vmEndTime - vmStartTime) / 1_000_000.0;
        double simTimeMs = (simEndTime - simStartTime) / 1_000_000.0;

        // Calculate GC overhead percentages
        double vmGCOverhead = (vmPauseMs / vmTimeMs) * 100.0;
        double simGCOverhead = (simulationPauseMs / simTimeMs) * 100.0;

        // Average pause time
        double avgPauseMs = totalCollections > 0 ? (double) totalPauseMs / totalCollections : 0.0;

        // Build result
        BenchmarkResult.Builder resultBuilder = new BenchmarkResult.Builder(getName())
                .profile(config.getProfile())
                .addMetric("total_gc_collections", totalCollections)
                .addMetric("total_gc_pause_ms", totalPauseMs)
                .addMetric("infrastructure_collections", infrastructureCollections)
                .addMetric("vm_collections", vmCollections)
                .addMetric("cloudlet_collections", cloudletCollections)
                .addMetric("simulation_collections", simulationCollections)
                .addMetric("infrastructure_pause_ms", infrastructurePauseMs)
                .addMetric("vm_pause_ms", vmPauseMs)
                .addMetric("cloudlet_pause_ms", cloudletPauseMs)
                .addMetric("simulation_pause_ms", simulationPauseMs)
                .addMetric("avg_pause_ms", avgPauseMs)
                .addMetric("vm_gc_overhead_percent", vmGCOverhead)
                .addMetric("simulation_gc_overhead_percent", simGCOverhead)
                .addMetadata("total_vms", String.valueOf(TARGET_VMS))
                .addMetadata("total_cloudlets", String.valueOf(cloudlets.size()));

        // Check if GC overhead is acceptable (< 10%)
        if (vmGCOverhead > 10.0 || simGCOverhead > 10.0) {
            resultBuilder.failureReason(
                    String.format("GC overhead too high: VM=%.2f%%, Sim=%.2f%% (threshold: 10%%)",
                            vmGCOverhead, simGCOverhead));
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

        // Final cleanup
        System.gc();
    }

    /**
     * Capture current GC statistics from all collectors
     */
    private GCStats captureGCStats() {
        long totalCollections = 0;
        long totalPauseTime = 0;

        for (GarbageCollectorMXBean gcBean : gcBeans) {
            totalCollections += gcBean.getCollectionCount();
            totalPauseTime += gcBean.getCollectionTime();
        }

        return new GCStats(totalCollections, totalPauseTime);
    }

    /**
     * Simple holder for GC statistics
     */
    private static class GCStats {
        final long totalCollections;
        final long totalPauseTime;  // milliseconds

        GCStats(long totalCollections, long totalPauseTime) {
            this.totalCollections = totalCollections;
            this.totalPauseTime = totalPauseTime;
        }
    }
}
