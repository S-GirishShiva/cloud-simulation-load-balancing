package com.cloudsimulation.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsAggregator class.
 * Tests aggregation logic (sum and mean), multi-algorithm tracking, and edge cases.
 */
public class MetricsAggregatorTest {
    private MetricsAggregator aggregator;

    @BeforeEach
    public void setup() {
        aggregator = new MetricsAggregator();
    }

    @Test
    public void testAddMetricsAccumulation() {
        AlgorithmMetrics metrics1 = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .timestamp(10.0)
            .totalMigrations(5)
            .build();

        AlgorithmMetrics metrics2 = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .timestamp(20.0)
            .totalMigrations(3)
            .build();

        aggregator.addMetrics(metrics1);
        aggregator.addMetrics(metrics2);

        AlgorithmMetrics aggregated = aggregator.getAggregated("threshold");

        assertNotNull(aggregated, "Aggregated metrics should not be null");
        assertEquals("threshold", aggregated.getAlgorithmName());
    }

    @Test
    public void testGetAggregatedReturnsNull() {
        AlgorithmMetrics aggregated = aggregator.getAggregated("nonexistent");

        assertNull(aggregated, "Should return null for unknown algorithm");
    }

    @Test
    public void testSumAggregation() {
        // Add 3 metrics with different migration counts
        for (int i = 0; i < 3; i++) {
            AlgorithmMetrics metrics = new AlgorithmMetrics.Builder()
                .algorithmName("test")
                .totalMigrations(5)
                .overloadEvents(2)
                .slaViolations(1)
                .build();

            aggregator.addMetrics(metrics);
        }

        AlgorithmMetrics aggregated = aggregator.getAggregated("test");

        assertEquals(15, aggregated.getTotalMigrations(),
            "Total migrations should be sum (5 + 5 + 5 = 15)");
        assertEquals(6, aggregated.getOverloadEvents(),
            "Overload events should be sum (2 + 2 + 2 = 6)");
        assertEquals(3, aggregated.getSlaViolations(),
            "SLA violations should be sum (1 + 1 + 1 = 3)");
    }

    @Test
    public void testMeanAggregation() {
        // Add 3 metrics with different utilization and decision times
        AlgorithmMetrics metrics1 = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .averageUtilization(0.6)
            .decisionTimeMs(10L)
            .timestamp(10.0)
            .build();

        AlgorithmMetrics metrics2 = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .averageUtilization(0.8)
            .decisionTimeMs(20L)
            .timestamp(20.0)
            .build();

        AlgorithmMetrics metrics3 = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .averageUtilization(0.7)
            .decisionTimeMs(15L)
            .timestamp(30.0)
            .build();

        aggregator.addMetrics(metrics1);
        aggregator.addMetrics(metrics2);
        aggregator.addMetrics(metrics3);

        AlgorithmMetrics aggregated = aggregator.getAggregated("test");

        assertEquals(0.7, aggregated.getAverageUtilization(), 0.01,
            "Average utilization should be mean ((0.6 + 0.8 + 0.7) / 3 = 0.7)");
        assertEquals(15L, aggregated.getDecisionTimeMs(),
            "Decision time should be mean ((10 + 20 + 15) / 3 = 15)");
        assertEquals(30.0, aggregated.getTimestamp(), 0.01,
            "Timestamp should be last value (30.0)");
    }

    @Test
    public void testMultiAlgorithmTracking() {
        // Add metrics for threshold algorithm
        AlgorithmMetrics threshold1 = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .totalMigrations(10)
            .timestamp(10.0)
            .build();

        aggregator.addMetrics(threshold1);

        // Add metrics for nsga2 algorithm
        AlgorithmMetrics nsga21 = new AlgorithmMetrics.Builder()
            .algorithmName("nsga2")
            .totalMigrations(5)
            .timestamp(10.0)
            .build();

        aggregator.addMetrics(nsga21);

        // Add metrics for hybrid algorithm
        AlgorithmMetrics hybrid1 = new AlgorithmMetrics.Builder()
            .algorithmName("hybrid")
            .totalMigrations(7)
            .timestamp(10.0)
            .build();

        aggregator.addMetrics(hybrid1);

        // Verify all algorithms are tracked
        AlgorithmMetrics thresholdAgg = aggregator.getAggregated("threshold");
        AlgorithmMetrics nsga2Agg = aggregator.getAggregated("nsga2");
        AlgorithmMetrics hybridAgg = aggregator.getAggregated("hybrid");

        assertNotNull(thresholdAgg, "Threshold metrics should exist");
        assertNotNull(nsga2Agg, "NSGA2 metrics should exist");
        assertNotNull(hybridAgg, "Hybrid metrics should exist");

        assertEquals(10, thresholdAgg.getTotalMigrations());
        assertEquals(5, nsga2Agg.getTotalMigrations());
        assertEquals(7, hybridAgg.getTotalMigrations());
    }

    @Test
    public void testGetTrackedAlgorithms() {
        // Add metrics for multiple algorithms
        aggregator.addMetrics(new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .build());

        aggregator.addMetrics(new AlgorithmMetrics.Builder()
            .algorithmName("nsga2")
            .build());

        aggregator.addMetrics(new AlgorithmMetrics.Builder()
            .algorithmName("hybrid")
            .build());

        List<String> tracked = aggregator.getTrackedAlgorithms();

        assertEquals(3, tracked.size(), "Should track 3 algorithms");
        assertTrue(tracked.contains("threshold"), "Should contain threshold");
        assertTrue(tracked.contains("nsga2"), "Should contain nsga2");
        assertTrue(tracked.contains("hybrid"), "Should contain hybrid");
    }

    @Test
    public void testEdgeCaseSingleMetric() {
        AlgorithmMetrics singleMetric = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .totalMigrations(10)
            .overloadEvents(5)
            .averageUtilization(0.75)
            .slaViolations(2)
            .decisionTimeMs(25L)
            .timestamp(100.0)
            .build();

        aggregator.addMetrics(singleMetric);

        AlgorithmMetrics aggregated = aggregator.getAggregated("test");

        assertEquals(10, aggregated.getTotalMigrations(),
            "Single metric migrations should equal original");
        assertEquals(5, aggregated.getOverloadEvents(),
            "Single metric overloads should equal original");
        assertEquals(0.75, aggregated.getAverageUtilization(), 0.01,
            "Single metric utilization should equal original");
        assertEquals(2, aggregated.getSlaViolations(),
            "Single metric SLA violations should equal original");
        assertEquals(25L, aggregated.getDecisionTimeMs(),
            "Single metric decision time should equal original");
        assertEquals(100.0, aggregated.getTimestamp(), 0.01,
            "Single metric timestamp should equal original");
    }

    @Test
    public void testEdgeCaseZeroValues() {
        // Add metrics with all zero values
        for (int i = 0; i < 3; i++) {
            AlgorithmMetrics metrics = new AlgorithmMetrics.Builder()
                .algorithmName("zero")
                .totalMigrations(0)
                .overloadEvents(0)
                .averageUtilization(0.0)
                .slaViolations(0)
                .decisionTimeMs(0L)
                .timestamp(i * 10.0)
                .build();

            aggregator.addMetrics(metrics);
        }

        AlgorithmMetrics aggregated = aggregator.getAggregated("zero");

        assertEquals(0, aggregated.getTotalMigrations(),
            "Zero migrations should aggregate to zero");
        assertEquals(0, aggregated.getOverloadEvents(),
            "Zero overloads should aggregate to zero");
        assertEquals(0.0, aggregated.getAverageUtilization(), 0.01,
            "Zero utilization should aggregate to zero");
        assertEquals(0, aggregated.getSlaViolations(),
            "Zero SLA violations should aggregate to zero");
        assertEquals(0L, aggregated.getDecisionTimeMs(),
            "Zero decision time should aggregate to zero");
    }

    @Test
    public void testConcurrentAlgorithmAddition() {
        // Simulate concurrent addition of metrics for same algorithm
        AlgorithmMetrics metrics1 = new AlgorithmMetrics.Builder()
            .algorithmName("concurrent")
            .totalMigrations(5)
            .build();

        AlgorithmMetrics metrics2 = new AlgorithmMetrics.Builder()
            .algorithmName("concurrent")
            .totalMigrations(10)
            .build();

        aggregator.addMetrics(metrics1);
        aggregator.addMetrics(metrics2);

        AlgorithmMetrics aggregated = aggregator.getAggregated("concurrent");

        assertEquals(15, aggregated.getTotalMigrations(),
            "Concurrent additions should aggregate correctly");
    }

    @Test
    public void testLargeNumberOfMetrics() {
        // Add 100 metrics
        for (int i = 0; i < 100; i++) {
            AlgorithmMetrics metrics = new AlgorithmMetrics.Builder()
                .algorithmName("stress")
                .totalMigrations(1)
                .overloadEvents(1)
                .averageUtilization(0.5)
                .slaViolations(0)
                .decisionTimeMs(10L)
                .timestamp(i * 1.0)
                .build();

            aggregator.addMetrics(metrics);
        }

        AlgorithmMetrics aggregated = aggregator.getAggregated("stress");

        assertEquals(100, aggregated.getTotalMigrations(),
            "Should aggregate 100 migrations");
        assertEquals(100, aggregated.getOverloadEvents(),
            "Should aggregate 100 overloads");
        assertEquals(0.5, aggregated.getAverageUtilization(), 0.01,
            "Average utilization should remain 0.5");
        assertEquals(10L, aggregated.getDecisionTimeMs(),
            "Average decision time should remain 10ms");
        assertEquals(99.0, aggregated.getTimestamp(), 0.01,
            "Last timestamp should be 99.0");
    }

    @Test
    public void testTimestampUsesLastValue() {
        double[] timestamps = {10.0, 20.0, 30.0, 40.0, 50.0};

        for (double timestamp : timestamps) {
            AlgorithmMetrics metrics = new AlgorithmMetrics.Builder()
                .algorithmName("timestamp-test")
                .timestamp(timestamp)
                .build();

            aggregator.addMetrics(metrics);
        }

        AlgorithmMetrics aggregated = aggregator.getAggregated("timestamp-test");

        assertEquals(50.0, aggregated.getTimestamp(), 0.01,
            "Timestamp should be last value (50.0), not average");
    }

    @Test
    public void testMixedPositiveNegativeValues() {
        // Test with realistic varying values
        AlgorithmMetrics[] metricsArray = {
            new AlgorithmMetrics.Builder()
                .algorithmName("mixed")
                .totalMigrations(10)
                .averageUtilization(0.3)
                .decisionTimeMs(5L)
                .timestamp(10.0)
                .build(),
            new AlgorithmMetrics.Builder()
                .algorithmName("mixed")
                .totalMigrations(0)
                .averageUtilization(0.9)
                .decisionTimeMs(50L)
                .timestamp(20.0)
                .build(),
            new AlgorithmMetrics.Builder()
                .algorithmName("mixed")
                .totalMigrations(5)
                .averageUtilization(0.6)
                .decisionTimeMs(20L)
                .timestamp(30.0)
                .build()
        };

        for (AlgorithmMetrics metrics : metricsArray) {
            aggregator.addMetrics(metrics);
        }

        AlgorithmMetrics aggregated = aggregator.getAggregated("mixed");

        assertEquals(15, aggregated.getTotalMigrations(),
            "Mixed migrations should sum correctly (10 + 0 + 5 = 15)");
        assertEquals(0.6, aggregated.getAverageUtilization(), 0.01,
            "Mixed utilization should average correctly ((0.3 + 0.9 + 0.6) / 3 = 0.6)");
        assertEquals(25L, aggregated.getDecisionTimeMs(),
            "Mixed decision time should average correctly ((5 + 50 + 20) / 3 = 25)");
    }
}
