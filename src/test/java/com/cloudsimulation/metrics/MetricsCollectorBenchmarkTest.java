package com.cloudsimulation.metrics;

import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Performance benchmark test for MetricsCollector.
 * Validates that metrics collection adds less than 1% overhead to simulation runtime.
 */
public class MetricsCollectorBenchmarkTest {
    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectorBenchmarkTest.class);
    private static final int SIMULATION_TICKS = 50;
    private static final double OVERHEAD_THRESHOLD = 1.0; // 1%

    @Test
    public void testMetricsCollectionOverhead() {
        logger.info("Starting metrics collection overhead benchmark test");

        // Run baseline simulation WITHOUT metrics collection
        long baselineTime = runSimulationWithoutMetrics();
        logger.info("Baseline time (without metrics): {} ms", baselineTime);

        // Run simulation WITH metrics collection
        long withMetricsTime = runSimulationWithMetrics();
        logger.info("With metrics time: {} ms", withMetricsTime);

        // Calculate overhead percentage
        double overhead = ((double) (withMetricsTime - baselineTime) / baselineTime) * 100.0;
        logger.info("Overhead: {}%", String.format("%.2f", overhead));

        // Validate AC #5: Less than 1% overhead
        assertTrue(overhead < OVERHEAD_THRESHOLD,
            String.format("Metrics overhead should be < %.1f%%, but was %.2f%%",
                OVERHEAD_THRESHOLD, overhead));

        logger.info("Benchmark test passed. Overhead: {}%", String.format("%.2f", overhead));
    }

    /**
     * Runs simulation WITHOUT metrics collection and measures execution time.
     *
     * @return Execution time in milliseconds
     */
    private long runSimulationWithoutMetrics() {
        CloudSimPlus simulation = new CloudSimPlus();
        CloudSimIntegration cloudSim = new CloudSimIntegration();

        // Create federation: 2 datacenters, 10 hosts each, 5 VMs per host = 100 VMs
        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
            .withDatacenters(2)
            .withHostsPerDatacenter(10)
            .withPesPerHost(4)
            .withVmsPerHost(5)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(federation));

        // Get VMs from broker
        List<Vm> vms = cloudSim.getBroker().getVmWaitingList();
        vms.addAll(cloudSim.getBroker().getVmExecList());

        // Create cloudlets for workload
        List<Cloudlet> cloudlets = createCloudlets(vms.size());
        cloudSim.submitCloudlets(cloudlets);

        // Start timer
        long startTime = System.nanoTime();

        // Run simulation for specified ticks
        cloudSim.start();
        for (int i = 0; i < SIMULATION_TICKS && !cloudSim.isFinished(); i++) {
            cloudSim.runFor(1.0);
        }

        // Stop timer
        long endTime = System.nanoTime();

        // Cleanup
        cloudSim.terminate();

        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Runs simulation WITH metrics collection every tick and measures execution time.
     * Uses OptimizedHierarchicalCollector with caching for better performance.
     *
     * @return Execution time in milliseconds
     */
    private long runSimulationWithMetrics() {
        CloudSimPlus simulation = new CloudSimPlus();
        CloudSimIntegration cloudSim = new CloudSimIntegration();
        OptimizedHierarchicalCollector collector = new OptimizedHierarchicalCollector();

        // Create identical federation: 2 datacenters, 10 hosts each, 5 VMs per host = 100 VMs
        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
            .withDatacenters(2)
            .withHostsPerDatacenter(10)
            .withPesPerHost(4)
            .withVmsPerHost(5)
            .build();

        cloudSim.initialize(simulation, Collections.singletonList(federation));

        // Get VMs from broker
        List<Vm> vms = cloudSim.getBroker().getVmWaitingList();
        vms.addAll(cloudSim.getBroker().getVmExecList());

        // Create identical cloudlets for workload
        List<Cloudlet> cloudlets = createCloudlets(vms.size());
        cloudSim.submitCloudlets(cloudlets);

        // Start timer
        long startTime = System.nanoTime();

        // Run simulation for specified ticks WITH metrics collection
        cloudSim.start();
        int snapshotCount = 0;
        for (int i = 0; i < SIMULATION_TICKS; i++) {
            // Collect metrics every tick (even if simulation finished)
            MetricsSnapshot snapshot = collector.collect(cloudSim);

            // Validate snapshot to prevent JVM optimization and ensure real work is done
            if (snapshot != null && snapshot.getTimestamp() >= 0) {
                snapshotCount++;
            }

            // Test cached metric getters (multiple calls to verify caching)
            double cpu1 = collector.getCachedAvgCpuUtilization(cloudSim);
            double cpu2 = collector.getCachedAvgCpuUtilization(cloudSim); // Should hit cache
            double mem1 = collector.getCachedAvgMemoryUtilization(cloudSim);
            double mem2 = collector.getCachedAvgMemoryUtilization(cloudSim); // Should hit cache
            long totalMem = collector.getCachedTotalAllocatedMemory(cloudSim); // Should hit cache

            // Validate values to prevent JVM optimization
            if (cpu1 >= 0.0 && cpu2 >= 0.0 && mem1 >= 0.0 && mem2 >= 0.0 && totalMem >= 0L) {
                // Values are valid
            }

            // Advance simulation if still running
            if (!cloudSim.isFinished()) {
                cloudSim.runFor(1.0);
            }
        }

        // Stop timer
        long endTime = System.nanoTime();

        // Log metrics collection for debugging
        logger.info("Collected {} metric snapshots during benchmark", snapshotCount);

        // Verify cache effectiveness (AC #7: >90% hit ratio expected)
        OptimizedHierarchicalCollector.CacheStatistics stats = collector.getCacheStatistics();
        double hitRatio = collector.getCacheHitRatio();
        logger.info("Cache statistics: {}", stats);
        logger.info("Cache hit ratio: {}%", String.format("%.2f", hitRatio * 100));

        // Assert cache hit ratio >90%
        assertTrue(hitRatio > 0.9,
            String.format("Cache hit ratio should be >90%%, but was %.2f%%", hitRatio * 100));

        // Cleanup
        cloudSim.terminate();

        return (endTime - startTime) / 1_000_000; // Convert to milliseconds
    }

    /**
     * Creates cloudlets for benchmark workload.
     *
     * @param count Number of cloudlets to create
     * @return List of cloudlets
     */
    private List<Cloudlet> createCloudlets(int count) {
        List<Cloudlet> cloudlets = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            // Create moderate workload cloudlets
            Cloudlet cloudlet = new CloudletSimple(5000, 2); // 5000 MI, 2 PEs
            cloudlet.setUtilizationModelCpu(new UtilizationModelDynamic(0.5)); // 50% CPU
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.3)); // 30% RAM
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.2));  // 20% BW
            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }
}
