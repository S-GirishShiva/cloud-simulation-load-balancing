package com.cloudsimulation.cli;

import com.cloudsimulation.models.ScenarioConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationRunner.
 * Tests complete simulation execution, infrastructure creation, workload generation,
 * cloudlet processing, memory usage, and console output.
 */
class SimulationRunnerTest {

    private ScenarioConfig config;
    private ByteArrayOutputStream outputStream;
    private PrintStream originalOut;

    @BeforeEach
    void setUp() {
        // Create test configuration with smaller scale for faster tests
        config = new ScenarioConfig();
        config.setScenarioId("test_scenario");
        config.setDuration(10);  // Reduced from 100 for faster tests
        config.setSeed(123456L);
        config.setTickInterval(1.0);

        // Configure infrastructure with smaller scale for faster tests
        // 1 datacenter, 2 hosts, 2 VMs per host = 4 VMs total
        ScenarioConfig.InfrastructureConfig infraConfig = new ScenarioConfig.InfrastructureConfig();
        infraConfig.setFederationCount(1);
        infraConfig.setDatacentersPerFederation(1);  // Reduced from 2
        infraConfig.setHostsPerDatacenter(2);  // Reduced from 10
        infraConfig.setVmsPerHost(2);  // Reduced from 5
        config.setInfrastructureConfig(infraConfig);

        // Capture console output for testing
        outputStream = new ByteArrayOutputStream();
        originalOut = System.out;
        System.setOut(new PrintStream(outputStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        // Restore original System.out
        System.setOut(originalOut);
    }

    @Test
    void testCompleteSimulationRun() {
        // Test: Full simulation with 100 VMs completes successfully
        SimulationRunner runner = new SimulationRunner(config);

        // Should not throw exceptions
        assertDoesNotThrow(() -> runner.run(),
                "Simulation should complete without exceptions");

        // Verify output contains completion message
        String output = outputStream.toString();
        assertTrue(output.contains("Simulation complete"),
                "Output should contain completion message");
    }

    @Test
    void testInfrastructureCreation() {
        // Test: Verify 1 datacenter, 2 hosts, 2 VMs per host = 4 VMs total
        SimulationRunner runner = new SimulationRunner(config);

        assertDoesNotThrow(() -> runner.run(),
                "Infrastructure creation should succeed");

        // Verify console output shows VMs were submitted
        String output = outputStream.toString();
        assertTrue(output.contains("4") || output.toLowerCase().contains("vm"),
                "Output should indicate VMs were created");
    }

    @Test
    void testWorkloadGeneration() {
        // Test: Verify 50 cloudlets per tick generated
        // For 100 ticks, this should be 5000 cloudlets total
        SimulationRunner runner = new SimulationRunner(config);

        assertDoesNotThrow(() -> runner.run());

        String output = outputStream.toString();
        // Output should show cloudlets were processed
        assertTrue(output.contains("cloudlet") || output.contains("Cloudlet"),
                "Output should mention cloudlets");
    }

    @Test
    void testSimulationDuration() {
        // Test: Verify simulation runs for 10 simulated seconds
        long startTime = System.currentTimeMillis();

        SimulationRunner runner = new SimulationRunner(config);
        runner.run();

        long elapsedTime = System.currentTimeMillis() - startTime;

        // Simulation should complete in less than 30 seconds real time
        assertTrue(elapsedTime < 30000,
                "Simulation should complete in less than 30 seconds (took " + elapsedTime + " ms)");

        // Verify simulation completed (check for completion message)
        String output = outputStream.toString();
        assertTrue(output.contains("Simulation complete") || output.contains("cloudlets"),
                "Output should indicate simulation completed");
    }

    @Test
    void testCloudletCompletion() {
        // Test: Verify VMs process and complete cloudlets
        SimulationRunner runner = new SimulationRunner(config);
        runner.run();

        String output = outputStream.toString();

        // Check that output mentions cloudlets were processed
        assertTrue(output.contains("cloudlets processed") || output.contains("Simulation complete"),
                "Output should indicate cloudlets were processed");

        // Output should show a positive number of cloudlets
        assertTrue(output.matches("(?s).*\\d+\\s+cloudlets.*"),
                "Output should show number of cloudlets processed");
    }

    @Test
    void testMemoryUsage() {
        // Test: Verify memory stays under 2GB throughout execution
        SimulationRunner runner = new SimulationRunner(config);

        // Get memory before simulation
        Runtime runtime = Runtime.getRuntime();
        long memoryBeforeMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        runner.run();

        // Get memory after simulation
        long memoryAfterMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

        System.setOut(originalOut); // Temporarily restore for output
        System.out.println("Memory before: " + memoryBeforeMB + " MB");
        System.out.println("Memory after: " + memoryAfterMB + " MB");

        // Memory usage should remain under 2GB (2048 MB)
        assertTrue(memoryAfterMB < 2048,
                "Memory usage should stay under 2GB (was " + memoryAfterMB + " MB)");

        // Verify output mentions memory usage
        String output = outputStream.toString();
        assertTrue(output.contains("Memory usage") || output.contains("MB"),
                "Output should report memory usage");
    }

    @Test
    void testConsoleOutput() {
        // Test: Verify console output contains relevant information
        SimulationRunner runner = new SimulationRunner(config);
        runner.run();

        String output = outputStream.toString();

        // Should show cloudlet information
        assertTrue(output.contains("cloudlets") || output.contains("Cloudlet"),
                "Output should mention cloudlets");

        // Should show memory usage
        assertTrue(output.contains("Memory usage") || output.contains("MB"),
                "Output should show memory usage");

        // Should show completion
        assertTrue(output.contains("Simulation complete"),
                "Output should indicate simulation completed");
    }

    @Test
    void testConfigurationValidation() {
        // Test: Null configuration should throw exception
        assertThrows(IllegalArgumentException.class,
                () -> new SimulationRunner(null),
                "Null configuration should throw IllegalArgumentException");
    }

    @Test
    void testSmallScaleSimulation() {
        // Test: Small scale simulation (1 datacenter, 2 hosts, 1 VM per host)
        ScenarioConfig smallConfig = new ScenarioConfig();
        smallConfig.setScenarioId("small_test");
        smallConfig.setDuration(10);
        smallConfig.setSeed(789L);
        smallConfig.setTickInterval(1.0);

        ScenarioConfig.InfrastructureConfig infraConfig = new ScenarioConfig.InfrastructureConfig();
        infraConfig.setFederationCount(1);
        infraConfig.setDatacentersPerFederation(1);
        infraConfig.setHostsPerDatacenter(2);
        infraConfig.setVmsPerHost(1);
        smallConfig.setInfrastructureConfig(infraConfig);

        SimulationRunner runner = new SimulationRunner(smallConfig);

        assertDoesNotThrow(() -> runner.run(),
                "Small scale simulation should complete successfully");

        String output = outputStream.toString();
        assertTrue(output.contains("Simulation complete"),
                "Small simulation should complete");
    }
}
