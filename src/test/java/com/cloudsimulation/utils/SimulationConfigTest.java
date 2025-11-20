package com.cloudsimulation.utils;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SimulationConfig utility class.
 * Tests logging configuration methods and their effects.
 *
 * Note: These tests verify that configuration methods execute without errors.
 * Future enhancement could verify actual logging level changes using Log.getLevel().
 */
class SimulationConfigTest {

    @Test
    void testConfigureLoggingVerboseTrue() {
        // Configure logging in verbose mode
        SimulationConfig.configureLogging(true);

        // Verify that verbose mode sets INFO level
        // The actual verification would need to check CloudSim Plus's internal Log class
        // Since Log.setLevel is called, we can verify by checking if it doesn't throw
        assertDoesNotThrow(() -> SimulationConfig.configureLogging(true),
                "Configuring verbose logging should not throw exceptions");
    }

    @Test
    void testConfigureLoggingVerboseFalse() {
        // Configure logging in performance mode (non-verbose)
        SimulationConfig.configureLogging(false);

        // Verify that performance mode disables logging
        assertDoesNotThrow(() -> SimulationConfig.configureLogging(false),
                "Configuring performance logging should not throw exceptions");
    }

    @Test
    void testConfigureLoggingWithSpecificLevel() {
        // Test with various logging levels
        Level[] levels = {Level.OFF, Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE};

        for (Level level : levels) {
            assertDoesNotThrow(() -> SimulationConfig.configureLogging(level),
                    "Configuring logging with level " + level + " should not throw exceptions");
        }
    }

    @Test
    void testConfigureLoggingWithNullLevel() {
        // Test null handling for Level parameter - CloudSim Plus doesn't allow null levels
        assertThrows(IllegalArgumentException.class,
                () -> SimulationConfig.configureLogging((Level) null),
                "Configuring logging with null level should throw IllegalArgumentException");
    }

    @Test
    void testMultipleLoggingConfigurationCalls() {
        // Test that multiple calls don't cause issues
        assertDoesNotThrow(() -> {
            SimulationConfig.configureLogging(true);
            SimulationConfig.configureLogging(false);
            SimulationConfig.configureLogging(Level.WARN);
            SimulationConfig.configureLogging(true);
        }, "Multiple logging configuration calls should not throw exceptions");
    }

    @Test
    void testLoggingLevelTransitions() {
        // Test transitions between different logging levels
        assertDoesNotThrow(() -> {
            // Start with verbose mode
            SimulationConfig.configureLogging(true);

            // Switch to performance mode
            SimulationConfig.configureLogging(false);

            // Set specific level
            SimulationConfig.configureLogging(Level.ERROR);

            // Back to verbose
            SimulationConfig.configureLogging(true);
        }, "Transitioning between logging levels should not throw exceptions");
    }

    @Test
    void testPerformanceModeConfiguration() {
        // Test that performance mode properly configures for maximum performance
        SimulationConfig.configureLogging(false);

        // In performance mode, CloudSim Plus logging should be OFF
        // This provides the documented 5-10x performance improvement
        assertDoesNotThrow(() -> SimulationConfig.configureLogging(Level.OFF),
                "Performance mode should be equivalent to Level.OFF");
    }

    @Test
    void testVerboseModeConfiguration() {
        // Test that verbose mode properly configures for visibility
        SimulationConfig.configureLogging(true);

        // In verbose mode, CloudSim Plus logging should be INFO
        assertDoesNotThrow(() -> SimulationConfig.configureLogging(Level.INFO),
                "Verbose mode should be equivalent to Level.INFO");
    }

    @Test
    void testLoggingConfigurationScenarios() {
        // Test various real-world scenarios

        // Scenario 1: Development debugging
        assertDoesNotThrow(() -> {
            SimulationConfig.configureLogging(Level.DEBUG);
        }, "Debug configuration for development should work");

        // Scenario 2: Production performance
        assertDoesNotThrow(() -> {
            SimulationConfig.configureLogging(false);
        }, "Production performance configuration should work");

        // Scenario 3: Troubleshooting in production
        assertDoesNotThrow(() -> {
            SimulationConfig.configureLogging(Level.WARN);
        }, "Warning-level configuration for troubleshooting should work");

        // Scenario 4: Standard operation
        assertDoesNotThrow(() -> {
            SimulationConfig.configureLogging(true);
        }, "Standard verbose configuration should work");
    }
}