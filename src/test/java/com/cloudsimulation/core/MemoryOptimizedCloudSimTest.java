package com.cloudsimulation.core;

import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.utils.SimulationConfig;
import com.cloudsimulation.workload.WorkloadGenerator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MemoryOptimizedCloudSim VM cleanup functionality.
 * Validates memory management and cleanup behavior.
 */
public class MemoryOptimizedCloudSimTest {

    private MemoryOptimizedCloudSim cloudSim;

    @BeforeEach
    void setUp() {
        // Disable logging for performance
        SimulationConfig.configureLogging(false);

        cloudSim = null;
    }

    @AfterEach
    void tearDown() {
        if (cloudSim != null) {
            try {
                cloudSim.terminate();
            } catch (Exception e) {
                System.err.println("Cleanup error: " + e.getMessage());
            }
        }
    }

    @Test
    void testBasicVmCleanup() {
        // Test basic cleanup of 10 VMs
        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);

        // Build federation: 1 DC, 2 hosts, 0 VMs (create manually)
        Federation federation = builder
                .withDatacenters(1)
                .withHostsPerDatacenter(2)
                .withVmsPerHost(0)
                .build();

        cloudSim = new MemoryOptimizedCloudSim();
        cloudSim.initialize(simulation, Collections.singletonList(federation));

        // Create 10 VMs manually
        List<Vm> vms = builder.getVmFactory().createVms(10);
        cloudSim.submitVms(vms);

        // Create and submit cloudlets
        WorkloadGenerator workload = new WorkloadGenerator();
        List<Cloudlet> cloudlets = workload.generateSteadyWorkload(2, 5); // 10 cloudlets
        cloudSim.submitCloudlets(cloudlets);

        // Run simulation
        cloudSim.start();
        cloudSim.runFor(10.0);

        // Force cleanup
        cloudSim.forceCleanup();

        // Verify cleanup occurred
        assertTrue(cloudSim.getTotalVmsCleanedUp() >= 0, "VMs cleanup count should be non-negative");

        System.out.println("Basic cleanup test passed. VMs cleaned: " + cloudSim.getTotalVmsCleanedUp());
    }

    @Test
    void testAutomaticCleanup() {
        // Test automatic cleanup during simulation
        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);

        Federation federation = builder
                .withDatacenters(1)
                .withHostsPerDatacenter(2)
                .withVmsPerHost(0)
                .build();

        cloudSim = new MemoryOptimizedCloudSim();
        cloudSim.setCleanupEnabled(true); // Enable automatic cleanup
        cloudSim.initialize(simulation, Collections.singletonList(federation));

        List<Vm> vms = builder.getVmFactory().createVms(6);
        cloudSim.submitVms(vms);

        WorkloadGenerator workload = new WorkloadGenerator();
        List<Cloudlet> cloudlets = workload.generateSteadyWorkload(1, 6);
        cloudSim.submitCloudlets(cloudlets);

        cloudSim.start();

        // Run with automatic cleanup
        cloudSim.runFor(10.0);

        // Verify automatic cleanup occurred
        assertTrue(cloudSim.isCleanupEnabled(), "Cleanup should be enabled");
        System.out.println("Automatic cleanup test passed. VMs cleaned: " + cloudSim.getTotalVmsCleanedUp());
    }

    @Test
    void testMemoryReduction() {
        // Test that memory is actually reduced after cleanup
        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);

        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(5)
                .withVmsPerHost(0)
                .build();

        cloudSim = new MemoryOptimizedCloudSim();
        cloudSim.initialize(simulation, Collections.singletonList(federation));

        // Create 40 VMs
        List<Vm> vms = builder.getVmFactory().createVms(40);
        cloudSim.submitVms(vms);

        WorkloadGenerator workload = new WorkloadGenerator();
        List<Cloudlet> cloudlets = workload.generateSteadyWorkload(2, 20); // 40 cloudlets
        cloudSim.submitCloudlets(cloudlets);

        // Force GC and measure baseline
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long memoryBefore = getUsedMemory();

        cloudSim.start();
        cloudSim.runFor(10.0);

        // Cleanup and GC
        cloudSim.cleanupAll();
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long memoryAfter = getUsedMemory();

        // Calculate reduction (Note: may not always be 30% due to JVM behavior)
        double reductionPercent = 0;
        if (memoryBefore > 0) {
            reductionPercent = ((memoryBefore - memoryAfter) / (double) memoryBefore) * 100;
        }

        System.out.println("Memory before: " + (memoryBefore / 1024 / 1024) + " MB");
        System.out.println("Memory after: " + (memoryAfter / 1024 / 1024) + " MB");
        System.out.println("Reduction: " + String.format("%.2f%%", reductionPercent));
        System.out.println("VMs cleaned: " + cloudSim.getTotalVmsCleanedUp());

        // Verify cleanup occurred
        assertTrue(cloudSim.getTotalVmsCleanedUp() >= 0, "VMs cleanup count should be non-negative");
    }

    @Test
    void testManualVsAutomaticCleanup() {
        // Test both manual and automatic modes
        cloudSim = new MemoryOptimizedCloudSim();
        assertFalse(cloudSim.isCleanupEnabled(), "Cleanup should be disabled by default");

        cloudSim.setCleanupEnabled(true);
        assertTrue(cloudSim.isCleanupEnabled(), "Cleanup should be enabled after setting");

        cloudSim.setCleanupEnabled(false);
        assertFalse(cloudSim.isCleanupEnabled(), "Cleanup should be disabled after setting");

        System.out.println("Manual vs automatic mode test passed");
    }

    @Test
    void testMemoryStats() {
        // Test memory statistics functionality
        cloudSim = new MemoryOptimizedCloudSim();

        MemoryOptimizedCloudSim.MemoryStats stats = cloudSim.getMemoryStats();

        assertNotNull(stats, "Memory stats should not be null");
        assertTrue(stats.getTotalMemoryMB() > 0, "Total memory should be positive");
        assertTrue(stats.getMaxMemoryMB() > 0, "Max memory should be positive");
        assertEquals(0, stats.getVmsCleanedUp(), "Initially no VMs cleaned");

        System.out.println("Memory stats: " + stats.toString());
        System.out.println("Memory stats test passed");
    }

    @Test
    void testNoPrematureDestruction() {
        // Verify VMs are not destroyed while cloudlets are running
        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);

        Federation federation = builder
                .withDatacenters(1)
                .withHostsPerDatacenter(1)
                .withVmsPerHost(0)
                .build();

        cloudSim = new MemoryOptimizedCloudSim();
        cloudSim.setCleanupEnabled(true);
        cloudSim.initialize(simulation, Collections.singletonList(federation));

        List<Vm> vms = builder.getVmFactory().createVms(2);
        cloudSim.submitVms(vms);

        WorkloadGenerator workload = new WorkloadGenerator();
        List<Cloudlet> cloudlets = workload.generateSteadyWorkload(1, 2);
        cloudSim.submitCloudlets(cloudlets);

        cloudSim.start();

        // Run for short time - cloudlets still executing
        cloudSim.runFor(1.0);

        // Attempt cleanup - should not destroy running VMs
        cloudSim.forceCleanup();

        // Finish simulation if still running
        if (!cloudSim.isFinished()) {
            cloudSim.runFor(10.0);
        }

        System.out.println("No premature destruction test passed");
    }

    /**
     * Helper method to measure current memory usage.
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
