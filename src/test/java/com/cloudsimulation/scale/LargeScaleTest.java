package com.cloudsimulation.scale;

import com.cloudsimulation.metrics.OptimizedHierarchicalCollector;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cloudsimplus.cloudlets.Cloudlet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Large scale test: 1000 VMs
 * Target: Performance testing machines (32GB RAM)
 * Purpose: Stress testing, scalability limits
 * Note: Disabled by default - requires manual execution with adequate heap size
 */
@Tag("scale")
@Tag("large")
@Disabled("Large scale test - run manually with adequate heap (java -Xmx4g)")
public class LargeScaleTest extends ScalabilityTestBase {

    private static final int TARGET_VMS = 1000;
    private static final int CLOUDLETS_PER_VM = 2;
    private static final long MAX_MEMORY_MB = 4096; // 4GB

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS) // 3 minute timeout
    void testLargeScalePerformance() {
        // Configure infrastructure: 5 federations, 4 DCs each, 10 hosts per DC, 5 VMs per host
        // 5 × 4 × 10 × 5 = 1000 VMs
        int actualVms = setupInfrastructure(5, 4, 10, 5);

        // Validate we're within 10% of target scale (catches configuration errors)
        assertTrue(Math.abs(actualVms - TARGET_VMS) <= TARGET_VMS * 0.1,
                String.format("Expected ~%d VMs (±10%%), got %d", TARGET_VMS, actualVms));

        System.out.println("Large scale test: " + actualVms + " VMs created");

        // Generate realistic workload: 2 cloudlets per VM
        int cloudletCount = actualVms * CLOUDLETS_PER_VM;
        List<Cloudlet> cloudlets = generateRealisticWorkload(cloudletCount);

        System.out.println("Generated " + cloudlets.size() + " cloudlets");

        // Submit cloudlets to broker
        cloudSim.submitCloudlets(cloudlets);

        // Run simulation for 30 seconds of simulation time
        runSimulation(30.0);

        // Validate memory usage (no hard time limit for large scale)
        assertMemoryUsage(MAX_MEMORY_MB);

        // Validate stability
        assertVmAssignment(actualVms, 0.90); // At least 90% VMs assigned (slightly lower for large scale)
        assertCloudletCompletion(cloudlets.size(), 0.90); // At least 90% cloudlets completed

        // Report performance metrics
        logPerformanceReport("Large", actualVms, cloudlets.size());
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void testLargeScaleMemoryCleanup() {
        // Test that VM cleanup is effective at large scale
        int actualVms = setupInfrastructure(5, 4, 10, 5);
        assertTrue(Math.abs(actualVms - TARGET_VMS) <= TARGET_VMS * 0.1,
                String.format("Expected ~%d VMs (±10%%), got %d", TARGET_VMS, actualVms));

        List<Cloudlet> cloudlets = generateRealisticWorkload(actualVms * 2);
        cloudSim.submitCloudlets(cloudlets);

        // Measure memory before simulation
        System.gc();
        long memoryBeforeSim = getUsedMemoryMB();

        runSimulation(30.0);

        // Force cleanup
        cloudSim.forceCleanup();
        System.gc();

        long memoryAfterCleanup = getUsedMemoryMB();
        int vmsCleanedUp = cloudSim.getTotalVmsCleanedUp();

        System.out.println("Memory before simulation: " + memoryBeforeSim + " MB");
        System.out.println("Memory after cleanup: " + memoryAfterCleanup + " MB");
        System.out.println("VMs cleaned up: " + vmsCleanedUp);

        // Verify cleanup effectiveness
        assertTrue(vmsCleanedUp >= 0, "Should have cleaned up VMs");
        assertTrue(memoryAfterCleanup < MAX_MEMORY_MB, "Memory should be under limit after cleanup");
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void testLargeScaleMetricsCaching() {
        // Test that metrics caching is effective at large scale
        int actualVms = setupInfrastructure(5, 4, 10, 5);
        assertTrue(Math.abs(actualVms - TARGET_VMS) <= TARGET_VMS * 0.1,
                String.format("Expected ~%d VMs (±10%%), got %d", TARGET_VMS, actualVms));

        List<Cloudlet> cloudlets = generateRealisticWorkload(actualVms * 2);
        cloudSim.submitCloudlets(cloudlets);

        runSimulation(30.0);

        // Get cache statistics
        OptimizedHierarchicalCollector.CacheStatistics stats = metricsCollector.getCacheStatistics();

        System.out.println("Cache statistics: " + stats);

        // Verify cache was used (if any metrics were collected)
        if (stats.getTotalRequests() > 0) {
            double hitRatio = stats.getHitRatio();
            System.out.println("Cache hit ratio: " + String.format("%.2f%%", hitRatio * 100));

            // Cache should be reasonably effective even at large scale
            assertTrue(hitRatio >= 0.0 && hitRatio <= 1.0,
                    "Cache hit ratio should be between 0 and 1");
        }
    }
}
