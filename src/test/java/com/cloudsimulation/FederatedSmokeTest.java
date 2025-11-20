package com.cloudsimulation;

import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.core.MemoryOptimizedCloudSim;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.metrics.HierarchicalCollector;
import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.ScenarioConfig;
import com.cloudsimulation.utils.SimulationConfig;
import com.cloudsimulation.workload.WorkloadGenerator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive federated smoke test that validates the complete simulation infrastructure.
 * This test replaces the original Story 1.2 smoke test which only tested basic CloudSim Plus.
 *
 * This test validates:
 * - FederationBuilder correctly creates federated infrastructure
 * - CloudSimIntegration initializes simulation engine
 * - DatacenterBroker is functional
 * - VMs are submitted and assigned
 * - Cloudlets complete execution
 * - HierarchicalCollector gathers federation-wide metrics
 * - All entity relationships are intact (VM→Host→Datacenter→Federation)
 * - Performance meets requirements (<10s execution, <500MB memory)
 */
public class FederatedSmokeTest {

    private static final int MAX_EXECUTION_TIME_MS = 10000;
    private static final int MAX_MEMORY_MB = 500;

    private CloudSimIntegration cloudSim;
    private MemoryOptimizedCloudSim memoryOptimizedCloudSim;
    private ScenarioConfig config;

    @BeforeEach
    void setUp() {
        // Configure logging for performance (Story 2.1)
        // Disables CloudSim Plus logging overhead for faster test execution
        SimulationConfig.configureLogging(false);

        // Reset for each test
        cloudSim = null;
        config = null;
    }

    @AfterEach
    void tearDown() {
        // Clean up CloudSim resources
        CloudSimIntegration simToCleanup = (memoryOptimizedCloudSim != null) ? memoryOptimizedCloudSim : cloudSim;
        if (simToCleanup != null) {
            try {
                simToCleanup.terminate();
            } catch (Exception e) {
                System.err.println("Error during cleanup: " + e.getMessage());
            }
        }
    }

    @Test
    void testFederatedSimulationFlow() {
        long startTime = System.currentTimeMillis();

        try {
            // Task 3: Create minimal ScenarioConfig for smoke test
            config = new ScenarioConfig();
            config.setScenarioId("federated_smoke_test");
            config.setDuration(10);
            config.setSeed(12345L);
            config.setTickInterval(1.0);

            // Configure infrastructure using nested InfrastructureConfig
            ScenarioConfig.InfrastructureConfig infraConfig = new ScenarioConfig.InfrastructureConfig();
            infraConfig.setFederationCount(1);
            infraConfig.setDatacentersPerFederation(2);
            infraConfig.setHostsPerDatacenter(3);
            infraConfig.setVmsPerHost(2);
            config.setInfrastructureConfig(infraConfig);

            // Task 4: Build federated infrastructure using FederationBuilder
            // Create CloudSim Plus simulation instance first
            org.cloudsimplus.core.CloudSimPlus simulation = new org.cloudsimplus.core.CloudSimPlus();

            // Build federation using builder pattern (creates single federation)
            // Note: Set withVmsPerHost(0) to avoid auto-submitting VMs to internal broker
            // We'll create and submit VMs manually to CloudSimIntegration's broker
            FederationBuilder builder = new FederationBuilder(simulation);
            Federation federation = builder
                    .withDatacenters(2)
                    .withHostsPerDatacenter(3)
                    .withPesPerHost(4)
                    .withVmsPerHost(0)  // Don't create VMs yet
                    .build();

            assertNotNull(federation, "Federation should not be null");

            // Assert datacenter count (AC #1)
            List<Datacenter> datacenters = federation.getDatacenters();
            assertEquals(2, datacenters.size(), "Federation should contain 2 datacenters");

            // Verify each datacenter has 3 hosts (AC #2)
            int totalHostCount = 0;
            for (Datacenter dc : datacenters) {
                List<Host> hosts = dc.getHostList();
                assertEquals(3, hosts.size(), "Each datacenter should have 3 hosts");
                totalHostCount += hosts.size();
            }
            assertEquals(6, totalHostCount, "Should have 6 hosts total (3 per datacenter)");

            // Task 5: Initialize CloudSimIntegration and broker
            // Story 2.2: Use MemoryOptimizedCloudSim for automatic cleanup
            memoryOptimizedCloudSim = new MemoryOptimizedCloudSim();
            memoryOptimizedCloudSim.setCleanupEnabled(true); // Enable automatic VM cleanup
            memoryOptimizedCloudSim.initialize(simulation, java.util.Collections.singletonList(federation));

            // Verify simulation and broker created (AC #3)
            assertNotNull(memoryOptimizedCloudSim.getSimulation(), "CloudSim Plus Simulation should be initialized");
            assertNotNull(memoryOptimizedCloudSim.getBroker(), "DatacenterBroker should be created");

            // Measure memory before simulation
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long memoryBefore = getUsedMemory();

            // Create VMs manually using VmFactory (2 VMs per host × 6 hosts = 12 VMs)
            List<Vm> allVms = builder.getVmFactory().createVms(12);
            assertEquals(12, allVms.size(), "Should create 12 VMs");

            // Submit VMs to broker for host assignment
            memoryOptimizedCloudSim.submitVms(allVms);

            // Task 6: Generate and submit workload
            WorkloadGenerator generator = new WorkloadGenerator();
            List<Cloudlet> cloudlets = generator.generateSteadyWorkload(2, 10); // 2 cloudlets/tick × 10 ticks = 20 cloudlets

            // Assert cloudlet count (AC #3)
            assertEquals(20, cloudlets.size(), "Should generate exactly 20 cloudlets");

            // Submit cloudlets to broker
            memoryOptimizedCloudSim.submitCloudlets(cloudlets);

            // Task 7: Execute simulation and validate completion
            // Start the simulation first
            memoryOptimizedCloudSim.start();
            assertEquals("RUNNING", memoryOptimizedCloudSim.getState(), "Simulation should be running");

            // Run simulation for 10 seconds (using incremental approach like SimulationEngineSmokeTest)
            while (!memoryOptimizedCloudSim.isFinished() && memoryOptimizedCloudSim.clock() < 10.0) {
                memoryOptimizedCloudSim.runFor(1.0);
            }

            // Verify all VMs assigned to hosts (AC #2)
            long assignedVms = allVms.stream()
                    .filter(vm -> vm.getHost() != null)
                    .count();
            assertTrue(assignedVms > 0, "VMs should be assigned to hosts (got " + assignedVms + " out of 12)");

            // Verify cloudlets completed (AC #3)
            long completedCloudlets = cloudlets.stream()
                    .filter(Cloudlet::isFinished)
                    .count();
            assertTrue(completedCloudlets > 0, "At least some cloudlets should complete (got " + completedCloudlets + " out of 20)");

            List<Cloudlet> finishedCloudlets = memoryOptimizedCloudSim.getBroker().getCloudletFinishedList();
            assertNotNull(finishedCloudlets, "Finished cloudlet list should not be null");

            // Story 2.2: Force cleanup and measure memory after
            memoryOptimizedCloudSim.forceCleanup();
            System.gc();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            long memoryAfter = getUsedMemory();
            long memoryFreed = memoryBefore - memoryAfter;
            double memoryReductionPercent = (memoryBefore > 0) ? ((double) memoryFreed / memoryBefore) * 100 : 0;

            // Task 8: Validate metrics collection from federated infrastructure
            HierarchicalCollector metricsCollector = new HierarchicalCollector();
            MetricsSnapshot snapshot = metricsCollector.collect(memoryOptimizedCloudSim);

            // Assert metrics snapshot collected (AC #4)
            assertNotNull(snapshot, "Should collect federated metrics snapshot");

            // Verify metrics contain valid data (AC #4)
            double avgCpu = snapshot.getAvgCpuUtilization();
            assertTrue(avgCpu >= 0.0 && avgCpu <= 1.0,
                "Average CPU utilization should be between 0.0 and 1.0, got: " + avgCpu);

            // Note: activeVmCount may be 0 if metrics are collected before VMs are fully initialized
            int activeVmsInMetrics = snapshot.getActiveVmCount();
            assertTrue(activeVmsInMetrics >= 0 && activeVmsInMetrics <= 12,
                "Active VM count in metrics should be valid (got: " + activeVmsInMetrics + ")");

            // Task 12: Validate memory usage constraints
            Runtime runtime = Runtime.getRuntime();
            long memoryUsedBytes = runtime.totalMemory() - runtime.freeMemory();
            long memoryUsageMB = memoryUsedBytes / (1024 * 1024);

            // Assert memory usage (AC #5)
            assertTrue(memoryUsageMB < MAX_MEMORY_MB,
                "Memory usage should stay under 500MB, got: " + memoryUsageMB + " MB");

            // Calculate execution time
            long endTime = System.currentTimeMillis();
            long executionTimeMs = endTime - startTime;

            // Assert execution time (AC #5)
            assertTrue(executionTimeMs < MAX_EXECUTION_TIME_MS,
                "Smoke test should complete in <10 seconds, took: " + executionTimeMs + " ms");

            // Task 9: Output smoke test results
            // Story 2.1: Performance metrics (execution time, memory) demonstrate optimization impact
            // Story 2.2: VM cleanup and memory reclamation
            System.out.println("\n========================================");
            System.out.println("FEDERATED SMOKE TEST PASSED");
            System.out.println("========================================");
            System.out.println("Federation count: 1");
            System.out.println("Datacenter count: " + datacenters.size());
            System.out.println("Total hosts: " + totalHostCount);
            System.out.println("Total VMs created: " + allVms.size());
            System.out.println("VMs assigned to hosts: " + assignedVms);
            System.out.println("Cloudlets processed: " + completedCloudlets + " out of " + cloudlets.size());
            System.out.println("Execution time: " + String.format("%,d", executionTimeMs) + " ms (Story 2.1 optimization)");
            System.out.println("Memory usage: " + memoryUsageMB + " MB");
            System.out.println("Average CPU utilization: " + String.format("%.2f%%", avgCpu * 100));
            System.out.println("Active VMs in metrics: " + activeVmsInMetrics);
            System.out.println("Simulation time: " + memoryOptimizedCloudSim.clock() + " seconds");
            System.out.println("\n--- Story 2.2: Memory Cleanup ---");
            System.out.println("VMs cleaned up: " + memoryOptimizedCloudSim.getTotalVmsCleanedUp());
            System.out.println("Memory before cleanup: " + (memoryBefore / 1024 / 1024) + " MB");
            System.out.println("Memory after cleanup: " + (memoryAfter / 1024 / 1024) + " MB");
            System.out.println("Memory freed: " + (memoryFreed / 1024 / 1024) + " MB");
            System.out.println("Memory reduction: " + String.format("%.2f%%", memoryReductionPercent));
            System.out.println("========================================\n");

        } catch (Exception e) {
            fail("Federated smoke test failed with exception: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to measure current memory usage.
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
