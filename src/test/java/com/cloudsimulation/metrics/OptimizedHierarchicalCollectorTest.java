package com.cloudsimulation.metrics;

import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.utils.SimulationConfig;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for OptimizedHierarchicalCollector caching functionality.
 * Tests cache expiration, invalidation, statistics tracking, and accuracy.
 */
public class OptimizedHierarchicalCollectorTest {

    private CloudSimPlus simulation;
    private CloudSimIntegration cloudSim;
    private OptimizedHierarchicalCollector collector;

    @BeforeEach
    void setUp() {
        // Disable logging for performance
        SimulationConfig.configureLogging(false);

        simulation = new CloudSimPlus();
        cloudSim = new CloudSimIntegration();
        collector = new OptimizedHierarchicalCollector();
    }

    @AfterEach
    void tearDown() {
        if (cloudSim != null) {
            try {
                cloudSim.terminate();
            } catch (Exception e) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    void testCacheHitOnRapidCalls() {
        // Test that rapid successive calls hit the cache
        setupSimulation(2, 3, 2); // 12 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // First call - cache miss
        double cpu1 = collector.getCachedAvgCpuUtilization(cloudSim);

        // Second call immediately - should hit cache
        double cpu2 = collector.getCachedAvgCpuUtilization(cloudSim);

        // Values should be identical
        assertEquals(cpu1, cpu2, 0.0001, "Cached values should match");

        // Verify cache statistics
        OptimizedHierarchicalCollector.CacheStatistics stats = collector.getCacheStatistics();
        assertEquals(2, stats.getTotalRequests(), "Should have 2 requests");
        assertEquals(1, stats.getHits(), "Should have 1 cache hit");
        assertEquals(1, stats.getMisses(), "Should have 1 cache miss");
        assertEquals(0.5, stats.getHitRatio(), 0.01, "Hit ratio should be 50%");
    }

    @Test
    void testCacheExpiration() throws InterruptedException {
        // Test that cache expires after configured duration
        collector = new OptimizedHierarchicalCollector(100); // 100ms cache duration
        setupSimulation(1, 2, 2); // 4 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // First call - cache miss (cold cache)
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Wait for cache to expire
        Thread.sleep(150);

        // Second call after expiration - should recalculate (another cache miss)
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Both calls should have recalculated (2 misses, 0 hits)
        OptimizedHierarchicalCollector.CacheStatistics stats = collector.getCacheStatistics();
        assertEquals(2, stats.getMisses(), "Should have 2 cache misses after expiration");
        assertEquals(0, stats.getHits(), "Should have 0 cache hits after expiration");
    }

    @Test
    void testCacheInvalidationOnVmChange() {
        // Test that cache invalidates when VM count changes
        setupSimulation(1, 2, 2); // Start with 4 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // Collect metrics to establish baseline
        collector.collect(cloudSim);

        // First call to get cached CPU - triggers cache
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Get initial statistics
        OptimizedHierarchicalCollector.CacheStatistics statsBefore = collector.getCacheStatistics();
        long requestsBefore = statsBefore.getTotalRequests();

        // Add VMs (simulated state change)
        FederationBuilder builder = new FederationBuilder(simulation);
        List<Vm> newVms = builder.getVmFactory().createVms(5);
        cloudSim.submitVms(newVms);

        // Run simulation if still active
        if (!cloudSim.isFinished()) {
            cloudSim.runFor(0.5);
        }

        // Collect again - should detect VM count change and invalidate
        collector.collect(cloudSim);

        // Second call to get CPU - should increment request count
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Cache should have more requests after VM state change
        OptimizedHierarchicalCollector.CacheStatistics statsAfter = collector.getCacheStatistics();
        assertTrue(statsAfter.getTotalRequests() > requestsBefore, "Should have more cache requests after VM change");
    }

    @Test
    void testManualCacheInvalidation() {
        // Test manual cache invalidation
        setupSimulation(1, 2, 2); // 4 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // First call - cache miss
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Manually invalidate cache
        collector.invalidateCache();

        // Second call - should be cache miss due to invalidation
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Both calls should be cache misses (manual invalidation cleared cache)
        OptimizedHierarchicalCollector.CacheStatistics stats = collector.getCacheStatistics();
        assertEquals(2, stats.getMisses(), "Should have 2 cache misses after manual invalidation");
    }

    @Test
    void testMemoryCachingAccuracy() {
        // Test that cached and fresh memory calculations match
        setupSimulation(2, 3, 2); // 12 VMs

        cloudSim.start();
        cloudSim.runFor(2.0);

        // Get cached values
        double cachedAvgMem = collector.getCachedAvgMemoryUtilization(cloudSim);
        long cachedTotalMem = collector.getCachedTotalAllocatedMemory(cloudSim);

        // Invalidate and recalculate
        collector.invalidateCache();

        double freshAvgMem = collector.getCachedAvgMemoryUtilization(cloudSim);
        long freshTotalMem = collector.getCachedTotalAllocatedMemory(cloudSim);

        // Values should match (within floating point precision)
        assertEquals(cachedAvgMem, freshAvgMem, 0.0001, "Cached and fresh avg memory should match");
        assertEquals(cachedTotalMem, freshTotalMem, "Cached and fresh total memory should match");
    }

    @Test
    void testCpuCachingAccuracy() {
        // Test that cached and fresh CPU calculations match
        setupSimulation(2, 5, 3); // 30 VMs

        cloudSim.start();
        cloudSim.runFor(3.0);

        // Get cached value
        double cachedCpu = collector.getCachedAvgCpuUtilization(cloudSim);

        // Invalidate and recalculate
        collector.invalidateCache();

        double freshCpu = collector.getCachedAvgCpuUtilization(cloudSim);

        // Values should match (within floating point precision)
        assertEquals(cachedCpu, freshCpu, 0.0001, "Cached and fresh CPU should match");
    }

    @Test
    void testStatisticsReset() {
        // Test statistics reset functionality
        setupSimulation(1, 2, 2); // 4 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // Generate some cache activity
        collector.getCachedAvgCpuUtilization(cloudSim);
        collector.getCachedAvgCpuUtilization(cloudSim);
        collector.getCachedAvgMemoryUtilization(cloudSim);

        OptimizedHierarchicalCollector.CacheStatistics statsBefore = collector.getCacheStatistics();
        assertTrue(statsBefore.getTotalRequests() > 0, "Should have requests before reset");

        // Reset statistics
        collector.resetStatistics();

        OptimizedHierarchicalCollector.CacheStatistics statsAfter = collector.getCacheStatistics();
        assertEquals(0, statsAfter.getTotalRequests(), "Requests should be 0 after reset");
        assertEquals(0, statsAfter.getHits(), "Hits should be 0 after reset");
        assertEquals(0, statsAfter.getMisses(), "Misses should be 0 after reset");
        assertEquals(0.0, statsAfter.getHitRatio(), 0.001, "Hit ratio should be 0 after reset");
    }

    @Test
    void testZeroVmScenario() {
        // Test edge case with no VMs
        setupSimulation(1, 2, 0); // 0 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // Should handle gracefully
        double cpu = collector.getCachedAvgCpuUtilization(cloudSim);
        double mem = collector.getCachedAvgMemoryUtilization(cloudSim);
        long totalMem = collector.getCachedTotalAllocatedMemory(cloudSim);

        assertTrue(cpu >= 0.0, "CPU should be non-negative");
        assertTrue(mem >= 0.0, "Memory should be non-negative");
        assertTrue(totalMem >= 0L, "Total memory should be non-negative");
    }

    @Test
    void testColdCacheFirstCall() {
        // Test first call on cold cache
        setupSimulation(1, 2, 2); // 4 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // First call ever on cold cache - should be cache miss
        collector.getCachedAvgCpuUtilization(cloudSim);

        // Verify first call results in cache miss (no previous cached value)
        OptimizedHierarchicalCollector.CacheStatistics stats = collector.getCacheStatistics();
        assertEquals(1, stats.getTotalRequests(), "Should have 1 request");
        assertEquals(0, stats.getHits(), "Should have 0 hits on cold cache");
        assertEquals(1, stats.getMisses(), "Should have 1 miss on cold cache");
    }

    @Test
    void testMultipleMetricsCacheIndependence() {
        // Test that CPU and memory caches are independent
        setupSimulation(1, 3, 2); // 6 VMs

        cloudSim.start();
        cloudSim.runFor(1.0);

        // Call CPU twice
        collector.getCachedAvgCpuUtilization(cloudSim);
        collector.getCachedAvgCpuUtilization(cloudSim); // Hit

        // Call memory twice
        collector.getCachedAvgMemoryUtilization(cloudSim);
        collector.getCachedAvgMemoryUtilization(cloudSim); // Hit

        OptimizedHierarchicalCollector.CacheStatistics stats = collector.getCacheStatistics();
        assertEquals(4, stats.getTotalRequests(), "Should have 4 total requests");
        assertEquals(2, stats.getHits(), "Should have 2 cache hits");
        assertEquals(2, stats.getMisses(), "Should have 2 cache misses");
        assertEquals(0.5, stats.getHitRatio(), 0.01, "Hit ratio should be 50%");
    }

    /**
     * Helper method to set up simulation infrastructure.
     */
    private void setupSimulation(int datacenters, int hostsPerDc, int vmsPerHost) {
        FederationBuilder builder = new FederationBuilder(simulation);
        Federation federation = builder
                .withDatacenters(datacenters)
                .withHostsPerDatacenter(hostsPerDc)
                .withPesPerHost(4)
                .withVmsPerHost(vmsPerHost)
                .build();

        cloudSim.initialize(simulation, Collections.singletonList(federation));

        // Create some workload if VMs exist
        if (vmsPerHost > 0) {
            int totalVms = datacenters * hostsPerDc * vmsPerHost;
            List<Cloudlet> cloudlets = createCloudlets(totalVms);
            cloudSim.submitCloudlets(cloudlets);
        }
    }

    /**
     * Helper method to create cloudlets.
     */
    private List<Cloudlet> createCloudlets(int count) {
        List<Cloudlet> cloudlets = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Cloudlet cloudlet = new CloudletSimple(1000, 2);
            cloudlet.setUtilizationModelCpu(new UtilizationModelDynamic(0.5));
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.3));
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.2));
            cloudlets.add(cloudlet);
        }
        return cloudlets;
    }
}
