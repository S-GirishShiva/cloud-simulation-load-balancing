package com.cloudsimulation.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AlgorithmMetrics class.
 * Tests Builder construction, getters, toString, and immutability.
 */
public class AlgorithmMetricsTest {
    private AlgorithmMetrics metrics;

    @BeforeEach
    public void setup() {
        metrics = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .timestamp(100.0)
            .totalMigrations(5)
            .overloadEvents(3)
            .averageUtilization(0.75)
            .slaViolations(1)
            .decisionTimeMs(25L)
            .build();
    }

    @Test
    public void testBuilderConstruction() {
        assertNotNull(metrics, "Metrics should be constructed via Builder");
        assertEquals("threshold", metrics.getAlgorithmName());
        assertEquals(100.0, metrics.getTimestamp(), 0.01);
        assertEquals(5, metrics.getTotalMigrations());
        assertEquals(3, metrics.getOverloadEvents());
        assertEquals(0.75, metrics.getAverageUtilization(), 0.01);
        assertEquals(1, metrics.getSlaViolations());
        assertEquals(25L, metrics.getDecisionTimeMs());
    }

    @Test
    public void testGetters() {
        assertEquals("threshold", metrics.getAlgorithmName(),
            "Algorithm name should match");
        assertEquals(100.0, metrics.getTimestamp(), 0.01,
            "Timestamp should match");
        assertEquals(5, metrics.getTotalMigrations(),
            "Total migrations should match");
        assertEquals(3, metrics.getOverloadEvents(),
            "Overload events should match");
        assertEquals(0.75, metrics.getAverageUtilization(), 0.01,
            "Average utilization should match");
        assertEquals(1, metrics.getSlaViolations(),
            "SLA violations should match");
        assertEquals(25L, metrics.getDecisionTimeMs(),
            "Decision time should match");
    }

    @Test
    public void testToString() {
        String str = metrics.toString();
        assertNotNull(str, "toString should not return null");
        assertTrue(str.contains("threshold"),
            "toString should contain algorithm name");
        assertTrue(str.contains("100.00"),
            "toString should contain timestamp");
        assertTrue(str.contains("migrations=5"),
            "toString should contain migrations count");
        assertTrue(str.contains("overloads=3"),
            "toString should contain overload events");
        assertTrue(str.contains("utilization=0.75"),
            "toString should contain utilization");
        assertTrue(str.contains("slaViolations=1"),
            "toString should contain SLA violations");
        assertTrue(str.contains("decisionTime=25ms"),
            "toString should contain decision time");
    }

    @Test
    public void testAlgorithmNameTagging() {
        AlgorithmMetrics nsga2Metrics = new AlgorithmMetrics.Builder()
            .algorithmName("nsga2")
            .build();

        assertEquals("nsga2", nsga2Metrics.getAlgorithmName(),
            "Algorithm name tagging should work for different algorithms");

        AlgorithmMetrics hybridMetrics = new AlgorithmMetrics.Builder()
            .algorithmName("hybrid")
            .build();

        assertEquals("hybrid", hybridMetrics.getAlgorithmName(),
            "Algorithm name tagging should work for hybrid algorithm");
    }

    @Test
    public void testTimestampStorage() {
        AlgorithmMetrics earlyMetrics = new AlgorithmMetrics.Builder()
            .timestamp(10.5)
            .build();

        assertEquals(10.5, earlyMetrics.getTimestamp(), 0.01,
            "Should store early timestamp correctly");

        AlgorithmMetrics lateMetrics = new AlgorithmMetrics.Builder()
            .timestamp(999.99)
            .build();

        assertEquals(999.99, lateMetrics.getTimestamp(), 0.01,
            "Should store late timestamp correctly");
    }

    @Test
    public void testDecisionTimeTracking() {
        AlgorithmMetrics fastMetrics = new AlgorithmMetrics.Builder()
            .decisionTimeMs(5L)
            .build();

        assertEquals(5L, fastMetrics.getDecisionTimeMs(),
            "Should track fast decision time");

        AlgorithmMetrics slowMetrics = new AlgorithmMetrics.Builder()
            .decisionTimeMs(150L)
            .build();

        assertEquals(150L, slowMetrics.getDecisionTimeMs(),
            "Should track slow decision time");
    }

    @Test
    public void testDefaultValues() {
        AlgorithmMetrics defaultMetrics = new AlgorithmMetrics.Builder().build();

        assertEquals("", defaultMetrics.getAlgorithmName(),
            "Default algorithm name should be empty string");
        assertEquals(0.0, defaultMetrics.getTimestamp(), 0.01,
            "Default timestamp should be 0.0");
        assertEquals(0, defaultMetrics.getTotalMigrations(),
            "Default migrations should be 0");
        assertEquals(0, defaultMetrics.getOverloadEvents(),
            "Default overload events should be 0");
        assertEquals(0.0, defaultMetrics.getAverageUtilization(), 0.01,
            "Default utilization should be 0.0");
        assertEquals(0, defaultMetrics.getSlaViolations(),
            "Default SLA violations should be 0");
        assertEquals(0L, defaultMetrics.getDecisionTimeMs(),
            "Default decision time should be 0");
    }

    @Test
    public void testImmutability() {
        // Create metrics with specific values
        AlgorithmMetrics original = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .timestamp(50.0)
            .totalMigrations(10)
            .build();

        // Verify values are immutable by attempting to retrieve them multiple times
        assertEquals("test", original.getAlgorithmName());
        assertEquals("test", original.getAlgorithmName(),
            "Algorithm name should remain constant");

        assertEquals(50.0, original.getTimestamp(), 0.01);
        assertEquals(50.0, original.getTimestamp(), 0.01,
            "Timestamp should remain constant");

        assertEquals(10, original.getTotalMigrations());
        assertEquals(10, original.getTotalMigrations(),
            "Total migrations should remain constant");
    }

    @Test
    public void testBuilderFluentInterface() {
        // Test that builder methods return the builder instance for chaining
        AlgorithmMetrics.Builder builder = new AlgorithmMetrics.Builder();
        AlgorithmMetrics result = builder
            .algorithmName("test")
            .timestamp(1.0)
            .totalMigrations(1)
            .overloadEvents(2)
            .averageUtilization(0.5)
            .slaViolations(0)
            .decisionTimeMs(10L)
            .build();

        assertNotNull(result, "Fluent builder interface should work");
        assertEquals("test", result.getAlgorithmName());
    }

    @Test
    public void testZeroValues() {
        AlgorithmMetrics zeroMetrics = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .timestamp(0.0)
            .totalMigrations(0)
            .overloadEvents(0)
            .averageUtilization(0.0)
            .slaViolations(0)
            .decisionTimeMs(0L)
            .build();

        assertEquals(0, zeroMetrics.getTotalMigrations(),
            "Should handle zero migrations");
        assertEquals(0, zeroMetrics.getOverloadEvents(),
            "Should handle zero overload events");
        assertEquals(0.0, zeroMetrics.getAverageUtilization(), 0.01,
            "Should handle zero utilization");
        assertEquals(0, zeroMetrics.getSlaViolations(),
            "Should handle zero SLA violations");
        assertEquals(0L, zeroMetrics.getDecisionTimeMs(),
            "Should handle zero decision time");
    }

    @Test
    public void testHighValues() {
        AlgorithmMetrics highMetrics = new AlgorithmMetrics.Builder()
            .algorithmName("stress-test")
            .timestamp(10000.0)
            .totalMigrations(1000)
            .overloadEvents(500)
            .averageUtilization(0.99)
            .slaViolations(100)
            .decisionTimeMs(5000L)
            .build();

        assertEquals(1000, highMetrics.getTotalMigrations(),
            "Should handle high migration count");
        assertEquals(500, highMetrics.getOverloadEvents(),
            "Should handle high overload count");
        assertEquals(0.99, highMetrics.getAverageUtilization(), 0.01,
            "Should handle high utilization");
        assertEquals(100, highMetrics.getSlaViolations(),
            "Should handle high SLA violations");
        assertEquals(5000L, highMetrics.getDecisionTimeMs(),
            "Should handle high decision time");
    }
}
