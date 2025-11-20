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
 * Benchmark measuring memory usage patterns and efficiency.
 * Tracks heap usage before/during/after simulation and calculates memory per VM.
 */
public class MemoryBenchmark implements BenchmarkRunner {

    private static final int TARGET_VMS = 100;
    private static final int CLOUDLETS_PER_VM = 2;

    private CloudSimPlus simulation;
    private List<Federation> federations;
    private CloudSimIntegration cloudSim;
    private MemoryOptimizedCloudSim memoryOptimizedCloudSim;
    private Runtime runtime;

    @Override
    public String getName() {
        return "memory";
    }

    @Override
    public String getDescription() {
        return "Measures memory usage patterns and memory efficiency (MB per VM)";
    }

    @Override
    public void setup() throws Exception {
        runtime = Runtime.getRuntime();
        // Force GC to get clean baseline
        System.gc();
        Thread.sleep(100);  // Give GC time to complete
    }

    /**
     * Warmup phase is skipped for CloudSim benchmarks.
     * CloudSim Plus simulations can only be started once per instance and cannot be reset or rerun.
     * Memory measurements are collected across 5 distinct phases within a single execution,
     * providing comprehensive memory profiling without requiring warmup.
     */
    @Override
    public void warmup(BenchmarkConfig config) {
        // Warmup not applicable - simulation cannot be rerun
    }

    @Override
    public BenchmarkResult run(BenchmarkConfig config) throws Exception {
        // Baseline memory measurement
        long baselineUsed = getUsedMemoryMB();

        // Phase 1: Infrastructure setup
        simulation = new CloudSimPlus();
        federations = new ArrayList<>();

        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(13)
                .withPesPerHost(4)
                .withVmsPerHost(0)
                .build();
        federations.add(federation);

        cloudSim = new CloudSimIntegration();
        memoryOptimizedCloudSim = new MemoryOptimizedCloudSim();
        memoryOptimizedCloudSim.setCleanupEnabled(true);

        cloudSim.initialize(simulation, List.of(federation));

        long afterInfrastructure = getUsedMemoryMB();

        // Phase 2: VM Creation
        builder = new FederationBuilder(simulation);
        List<Vm> vms = builder.getVmFactory().createVms(TARGET_VMS);
        cloudSim.submitVms(vms);

        long afterVMs = getUsedMemoryMB();

        // Phase 3: Cloudlet Creation
        WorkloadGenerator workloadGenerator = new WorkloadGenerator();
        List<Cloudlet> cloudlets = workloadGenerator.generateSteadyWorkload(
                CLOUDLETS_PER_VM, TARGET_VMS);
        cloudSim.submitCloudlets(cloudlets);

        long afterCloudlets = getUsedMemoryMB();

        // Phase 4: Simulation Execution
        cloudSim.start();

        long afterSimulation = getUsedMemoryMB();
        long peakMemory = afterSimulation;

        // Phase 5: Cleanup
        memoryOptimizedCloudSim.forceCleanup();
        System.gc();
        Thread.sleep(100);

        long afterCleanup = getUsedMemoryMB();

        // Calculate metrics
        long infrastructureMemory = afterInfrastructure - baselineUsed;
        long vmMemory = afterVMs - afterInfrastructure;
        long cloudletMemory = afterCloudlets - afterVMs;
        long totalUsed = afterSimulation - baselineUsed;
        long memoryRecovered = afterSimulation - afterCleanup;

        double mbPerVm = (double) vmMemory / TARGET_VMS;
        double mbPerCloudlet = (double) cloudletMemory / cloudlets.size();
        double cleanupEfficiency = (double) memoryRecovered / totalUsed * 100.0;

        // Build result
        BenchmarkResult.Builder resultBuilder = new BenchmarkResult.Builder(getName())
                .profile(config.getProfile())
                .addMetric("baseline_memory_mb", baselineUsed)
                .addMetric("infrastructure_memory_mb", infrastructureMemory)
                .addMetric("vm_memory_mb", vmMemory)
                .addMetric("cloudlet_memory_mb", cloudletMemory)
                .addMetric("peak_memory_mb", peakMemory)
                .addMetric("after_cleanup_mb", afterCleanup)
                .addMetric("memory_recovered_mb", memoryRecovered)
                .addMetric("mb_per_vm", mbPerVm)
                .addMetric("mb_per_cloudlet", mbPerCloudlet)
                .addMetric("cleanup_efficiency_percent", cleanupEfficiency)
                .addMetadata("total_vms", String.valueOf(TARGET_VMS))
                .addMetadata("total_cloudlets", String.valueOf(cloudlets.size()));

        // Check if memory per VM is within acceptable range (< 3MB per VM)
        if (mbPerVm > 3.0) {
            resultBuilder.failureReason(
                    String.format("Memory per VM %.2f MB exceeds threshold of 3 MB", mbPerVm));
        }

        return resultBuilder.build();
    }

    @Override
    public void teardown() throws Exception {
        if (cloudSim != null) {
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
     * Get current used memory in MB
     */
    private long getUsedMemoryMB() {
        return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }
}
