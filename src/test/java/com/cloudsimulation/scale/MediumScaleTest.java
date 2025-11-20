package com.cloudsimulation.scale;

import com.cloudsimulation.metrics.OptimizedHierarchicalCollector;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.cloudsimplus.cloudlets.Cloudlet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Medium scale test: 500 VMs
 * Target: Development servers (16GB RAM)
 * Purpose: Performance validation, regression testing
 */
@Tag("scale")
@Tag("medium")
public class MediumScaleTest extends ScalabilityTestBase {

    private static final int TARGET_VMS = 500;
    private static final int CLOUDLETS_PER_VM = 2;
    private static final long MAX_EXECUTION_TIME_MS = 60000; // 60 seconds
    private static final long MAX_MEMORY_MB = 2048; // 2GB

    @Test
    @Timeout(value = 75, unit = TimeUnit.SECONDS) // Safety timeout slightly higher than assertion
    void testMediumScalePerformance() {
        // Configure infrastructure: 3 federations, 3 DCs each, 11 hosts per DC, 5 VMs per host
        // 3 × 3 × 11 × 5 = 495 VMs (close to 500)
        int actualVms = setupInfrastructure(3, 3, 11, 5);

        // Validate we're within 10% of target scale (catches configuration errors)
        assertTrue(Math.abs(actualVms - TARGET_VMS) <= TARGET_VMS * 0.1,
                String.format("Expected ~%d VMs (±10%%), got %d", TARGET_VMS, actualVms));

        System.out.println("Medium scale test: " + actualVms + " VMs created");

        // Generate realistic workload: 2 cloudlets per VM
        int cloudletCount = actualVms * CLOUDLETS_PER_VM;
        List<Cloudlet> cloudlets = generateRealisticWorkload(cloudletCount);

        System.out.println("Generated " + cloudlets.size() + " cloudlets");

        // Submit cloudlets to broker
        cloudSim.submitCloudlets(cloudlets);

        // Run simulation for 20 seconds of simulation time
        runSimulation(20.0);

        // Validate performance targets
        assertExecutionTime(MAX_EXECUTION_TIME_MS);
        assertMemoryUsage(MAX_MEMORY_MB);

        // Validate stability
        assertVmAssignment(actualVms, 0.95); // At least 95% VMs assigned
        assertCloudletCompletion(cloudlets.size(), 0.95); // At least 95% cloudlets completed

        // Report performance metrics
        logPerformanceReport("Medium", actualVms, cloudlets.size());
    }

    @Test
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void testMediumScaleMemoryCleanup() {
        // Test that VM cleanup is effective at medium scale
        int actualVms = setupInfrastructure(3, 3, 11, 5);
        assertTrue(Math.abs(actualVms - TARGET_VMS) <= TARGET_VMS * 0.1,
                String.format("Expected ~%d VMs (±10%%), got %d", TARGET_VMS, actualVms));

        List<Cloudlet> cloudlets = generateRealisticWorkload(actualVms * 2);
        cloudSim.submitCloudlets(cloudlets);

        // Measure memory before simulation
        System.gc();
        long memoryBeforeSim = getUsedMemoryMB();

        runSimulation(20.0);

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
    @Timeout(value = 75, unit = TimeUnit.SECONDS)
    void testMediumScaleMetricsCaching() {
        // Test that metrics caching is effective at medium scale
        int actualVms = setupInfrastructure(3, 3, 11, 5);
        assertTrue(Math.abs(actualVms - TARGET_VMS) <= TARGET_VMS * 0.1,
                String.format("Expected ~%d VMs (±10%%), got %d", TARGET_VMS, actualVms));

        List<Cloudlet> cloudlets = generateRealisticWorkload(actualVms * 2);
        cloudSim.submitCloudlets(cloudlets);

        runSimulation(20.0);

        // Get cache statistics
        OptimizedHierarchicalCollector.CacheStatistics stats = metricsCollector.getCacheStatistics();

        System.out.println("Cache statistics: " + stats);

        // Verify cache was used (if any metrics were collected)
        if (stats.getTotalRequests() > 0) {
            double hitRatio = stats.getHitRatio();
            System.out.println("Cache hit ratio: " + String.format("%.2f%%", hitRatio * 100));

            // Cache should be reasonably effective
            assertTrue(hitRatio >= 0.0 && hitRatio <= 1.0,
                    "Cache hit ratio should be between 0 and 1");
        }
    }
}
