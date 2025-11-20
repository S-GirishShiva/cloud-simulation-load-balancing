package com.cloudsimulation.scale;

import com.cloudsimulation.core.MemoryOptimizedCloudSim;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.metrics.OptimizedHierarchicalCollector;
import com.cloudsimulation.utils.SimulationConfig;
import com.cloudsimulation.workload.WorkloadGenerator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base class for scalability tests.
 * Provides common setup, measurement utilities, and assertion methods.
 */
public abstract class ScalabilityTestBase {

    protected CloudSimPlus simulation;
    protected MemoryOptimizedCloudSim cloudSim;
    protected OptimizedHierarchicalCollector metricsCollector;
    protected List<Federation> federations;

    // Performance tracking
    protected long testStartTime;
    protected long simulationStartTime;
    protected long simulationEndTime;
    protected long memoryBefore;
    protected long memoryAfter;

    @BeforeEach
    void setUp() {
        // Disable logging for performance (Story 2.1)
        SimulationConfig.configureLogging(false);

        // Initialize components
        simulation = new CloudSimPlus();
        cloudSim = new MemoryOptimizedCloudSim();
        cloudSim.setCleanupEnabled(true); // Enable automatic VM cleanup (Story 2.2)
        metricsCollector = new OptimizedHierarchicalCollector(); // Use optimized collector (Story 2.3)
        federations = new ArrayList<>();

        // Start test timing
        testStartTime = System.nanoTime();

        // Measure baseline memory
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        memoryBefore = getUsedMemoryBytes();
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

        // Force cleanup and measure final memory
        System.gc();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        memoryAfter = getUsedMemoryBytes();
    }

    /**
     * Creates federation infrastructure with specified configuration.
     *
     * @param federationCount Number of federations
     * @param datacentersPerFederation Datacenters per federation
     * @param hostsPerDatacenter Hosts per datacenter
     * @param vmsPerHost VMs per host
     * @return Total number of VMs created
     */
    protected int setupInfrastructure(int federationCount, int datacentersPerFederation,
                                      int hostsPerDatacenter, int vmsPerHost) {
        // Create all federations first (without VMs)
        for (int i = 0; i < federationCount; i++) {
            FederationBuilder builder = new FederationBuilder(simulation);
            // Don't create VMs in builder - we'll create them manually to submit to CloudSim's broker
            Federation federation = builder
                    .withDatacenters(datacentersPerFederation)
                    .withHostsPerDatacenter(hostsPerDatacenter)
                    .withPesPerHost(4)
                    .withVmsPerHost(0)  // Don't auto-create VMs
                    .build();

            federations.add(federation);
        }

        // Initialize CloudSim with federations
        cloudSim.initialize(simulation, federations);

        // Create and submit VMs to CloudSim's broker (must be done after initialization)
        int totalVms = 0;
        if (vmsPerHost > 0) {
            int vmsForEachFederation = datacentersPerFederation * hostsPerDatacenter * vmsPerHost;
            for (int i = 0; i < federationCount; i++) {
                FederationBuilder builder = new FederationBuilder(simulation);
                List<Vm> vms = builder.getVmFactory().createVms(vmsForEachFederation);
                cloudSim.submitVms(vms);
                totalVms += vms.size();
            }
        }

        return totalVms;
    }

    /**
     * Generates realistic workload with varied characteristics.
     *
     * @param cloudletCount Number of cloudlets to generate
     * @return List of cloudlets
     */
    protected List<Cloudlet> generateRealisticWorkload(int cloudletCount) {
        WorkloadGenerator generator = new WorkloadGenerator();
        // Use steady workload pattern - generates cloudlets over simulation time
        int cloudletsPerTick = Math.max(1, cloudletCount / 10);
        return generator.generateSteadyWorkload(cloudletsPerTick, 10);
    }

    /**
     * Runs simulation until completion or timeout.
     *
     * @param duration Maximum simulation duration in seconds
     */
    protected void runSimulation(double duration) {
        simulationStartTime = System.nanoTime();

        cloudSim.start();

        // Run simulation in incremental steps
        double currentTime = 0.0;
        double stepSize = 1.0;

        while (currentTime < duration && !cloudSim.isFinished()) {
            cloudSim.runFor(stepSize);
            currentTime += stepSize;

            // Collect metrics periodically
            if (currentTime % 5.0 == 0) {
                metricsCollector.collect(cloudSim);
            }
        }

        simulationEndTime = System.nanoTime();
    }

    /**
     * Gets current heap memory usage in bytes.
     *
     * @return Used memory in bytes
     */
    protected long getUsedMemoryBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Gets current heap memory usage in megabytes.
     *
     * @return Used memory in MB
     */
    protected long getUsedMemoryMB() {
        return getUsedMemoryBytes() / (1024 * 1024);
    }

    /**
     * Gets peak memory usage during test in megabytes.
     *
     * @return Peak memory in MB
     */
    protected long getPeakMemoryMB() {
        return Math.max(memoryBefore, memoryAfter) / (1024 * 1024);
    }

    /**
     * Gets total test execution time in milliseconds.
     *
     * @return Execution time in ms
     */
    protected long getTestExecutionTimeMs() {
        return (System.nanoTime() - testStartTime) / 1_000_000;
    }

    /**
     * Gets simulation execution time in milliseconds.
     *
     * @return Simulation time in ms
     */
    protected long getSimulationTimeMs() {
        if (simulationEndTime == 0) {
            return 0;
        }
        return (simulationEndTime - simulationStartTime) / 1_000_000;
    }

    /**
     * Asserts that execution time is within specified limit.
     *
     * @param maxTimeMs Maximum allowed time in milliseconds
     */
    protected void assertExecutionTime(long maxTimeMs) {
        long actualTime = getTestExecutionTimeMs();
        assertTrue(actualTime < maxTimeMs,
                String.format("Execution time %dms exceeded limit %dms", actualTime, maxTimeMs));
    }

    /**
     * Asserts that memory usage is within specified limit.
     *
     * @param maxMemoryMB Maximum allowed memory in MB
     */
    protected void assertMemoryUsage(long maxMemoryMB) {
        long actualMemory = getUsedMemoryMB();
        assertTrue(actualMemory < maxMemoryMB,
                String.format("Memory usage %dMB exceeded limit %dMB", actualMemory, maxMemoryMB));
    }

    /**
     * Asserts that VM assignment rate meets minimum threshold.
     *
     * @param expectedVms Total VMs created
     * @param minAssignmentRate Minimum assignment rate (0.0 to 1.0)
     */
    protected void assertVmAssignment(int expectedVms, double minAssignmentRate) {
        // Get VMs from broker and count how many have been assigned to hosts
        List<Vm> brokerVms = cloudSim.getBroker().getVmCreatedList();
        long assignedVms = brokerVms.stream()
                .filter(vm -> vm.getHost() != null && vm.getHost() != Host.NULL)
                .count();

        double actualRate = (double) assignedVms / expectedVms;
        assertTrue(actualRate >= minAssignmentRate,
                String.format("VM assignment rate %.2f%% below threshold %.2f%% (%d/%d VMs assigned)",
                        actualRate * 100, minAssignmentRate * 100, assignedVms, expectedVms));
    }

    /**
     * Asserts that cloudlet completion rate meets minimum threshold.
     *
     * @param expectedCloudlets Total cloudlets submitted
     * @param minCompletionRate Minimum completion rate (0.0 to 1.0)
     */
    protected void assertCloudletCompletion(int expectedCloudlets, double minCompletionRate) {
        List<Cloudlet> finishedCloudlets = cloudSim.getBroker().getCloudletFinishedList();
        double actualRate = (double) finishedCloudlets.size() / expectedCloudlets;

        assertTrue(actualRate >= minCompletionRate,
                String.format("Cloudlet completion rate %.2f%% below threshold %.2f%%",
                        actualRate * 100, minCompletionRate * 100));
    }

    /**
     * Logs performance report to console.
     *
     * @param scaleName Scale name (e.g., "Small", "Medium", "Large")
     * @param vmCount Number of VMs
     * @param cloudletCount Number of cloudlets
     */
    protected void logPerformanceReport(String scaleName, int vmCount, int cloudletCount) {
        long executionTime = getTestExecutionTimeMs();
        long simulationTime = getSimulationTimeMs();
        long memoryUsed = getUsedMemoryMB();
        long peakMemory = getPeakMemoryMB();

        double vmsPerSecond = vmCount / (simulationTime / 1000.0);
        double cloudletsPerSecond = cloudletCount / (simulationTime / 1000.0);

        int vmsCleanedUp = cloudSim.getTotalVmsCleanedUp();

        System.out.println("\n========================================");
        System.out.println(scaleName.toUpperCase() + " SCALE TEST PERFORMANCE REPORT");
        System.out.println("========================================");
        System.out.println("Infrastructure:");
        System.out.println("  Federations: " + federations.size());
        System.out.println("  VMs: " + vmCount);
        System.out.println("  Cloudlets: " + cloudletCount);
        System.out.println("\nTiming:");
        System.out.println("  Total execution time: " + executionTime + " ms");
        System.out.println("  Simulation time: " + simulationTime + " ms");
        System.out.println("\nMemory:");
        System.out.println("  Current usage: " + memoryUsed + " MB");
        System.out.println("  Peak usage: " + peakMemory + " MB");
        System.out.println("  VMs cleaned up: " + vmsCleanedUp);
        System.out.println("\nThroughput:");
        System.out.println(String.format("  VMs/second: %.2f", vmsPerSecond));
        System.out.println(String.format("  Cloudlets/second: %.2f", cloudletsPerSecond));

        // Cache statistics
        OptimizedHierarchicalCollector.CacheStatistics cacheStats = metricsCollector.getCacheStatistics();
        System.out.println("\nCache Performance:");
        System.out.println("  " + cacheStats.toString());
        System.out.println("========================================\n");
    }
}
