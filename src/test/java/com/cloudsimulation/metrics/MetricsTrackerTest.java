package com.cloudsimulation.metrics;

import com.cloudsimulation.models.LoadBalancingPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MetricsTracker class.
 * Tests per-cycle metrics tracking, SLA violation detection, and overload event calculation.
 */
public class MetricsTrackerTest {
    private MetricsTracker tracker;
    private LoadBalancingPlan plan;
    private MetricsSnapshot snapshot;

    @BeforeEach
    public void setup() {
        tracker = new MetricsTracker();

        // Create test LoadBalancingPlan
        plan = new LoadBalancingPlan.Builder()
            .algorithmName("threshold")
            .timestamp(50.0)
            .migrations(new ArrayList<>()) // 0 migrations
            .computationTime(20L)
            .build();

        // Create test MetricsSnapshot
        snapshot = new MetricsSnapshot(50.0, 0.6, 0.5, 2, 1, 0, 10, 0.0);
    }

    @Test
    public void testRecordEvaluation() {
        AlgorithmMetrics metrics = tracker.recordEvaluation(plan, snapshot, 20L);

        assertNotNull(metrics, "Metrics should not be null");
        assertEquals("threshold", metrics.getAlgorithmName(),
            "Algorithm name should match plan");
        assertEquals(50.0, metrics.getTimestamp(), 0.01,
            "Timestamp should match plan");
        assertEquals(0, metrics.getTotalMigrations(),
            "Migrations should match plan migration count");
        assertEquals(2, metrics.getOverloadEvents(),
            "Overload events should match snapshot");
        assertEquals(0.6, metrics.getAverageUtilization(), 0.01,
            "Utilization should match snapshot");
        assertEquals(20L, metrics.getDecisionTimeMs(),
            "Decision time should match input");
    }

    @Test
    public void testOverloadEventCalculation() {
        MetricsSnapshot overloadSnapshot = new MetricsSnapshot(
            100.0, 0.9, 0.8, 5, 0, 0, 10, 0.0
        );

        LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
            .algorithmName("test")
            .timestamp(100.0)
            .build();

        AlgorithmMetrics metrics = tracker.recordEvaluation(testPlan, overloadSnapshot, 15L);

        assertEquals(5, metrics.getOverloadEvents(),
            "Should capture overload events from snapshot");
    }

    @Test
    public void testSlaViolationDetection() {
        // Trigger 5 consecutive overload events
        for (int i = 0; i < 5; i++) {
            MetricsSnapshot overloadSnapshot = new MetricsSnapshot(
                i, 0.9, 0.8, 5, 0, 0, 10, 0.0
            );
            LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
                .algorithmName("test")
                .timestamp(i)
                .build();

            AlgorithmMetrics metrics = tracker.recordEvaluation(testPlan, overloadSnapshot, 10L);

            if (i < 4) {
                assertEquals(0, metrics.getSlaViolations(),
                    "No SLA violation before 5 consecutive overload ticks");
            } else {
                assertEquals(1, metrics.getSlaViolations(),
                    "SLA violation should be detected after 5 consecutive overload ticks");
            }
        }
    }

    @Test
    public void testDecisionTimeRecording() {
        AlgorithmMetrics fastMetrics = tracker.recordEvaluation(plan, snapshot, 5L);
        assertEquals(5L, fastMetrics.getDecisionTimeMs(),
            "Should record fast decision time");

        LoadBalancingPlan slowPlan = new LoadBalancingPlan.Builder()
            .algorithmName("slow")
            .timestamp(60.0)
            .build();

        AlgorithmMetrics slowMetrics = tracker.recordEvaluation(slowPlan, snapshot, 150L);
        assertEquals(150L, slowMetrics.getDecisionTimeMs(),
            "Should record slow decision time");
    }

    @Test
    public void testPerCycleMetricsGeneration() {
        // Record multiple evaluations
        for (int i = 0; i < 3; i++) {
            LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
                .algorithmName("test")
                .timestamp(i * 10.0)
                .build();

            MetricsSnapshot testSnapshot = new MetricsSnapshot(
                i * 10.0, 0.5 + i * 0.1, 0.5, i, 0, i, 10, 0.0
            );

            tracker.recordEvaluation(testPlan, testSnapshot, 10L);
        }

        List<AlgorithmMetrics> perCycleMetrics = tracker.getPerCycleMetrics();

        assertEquals(3, perCycleMetrics.size(),
            "Should have recorded 3 cycles");

        // Verify first cycle
        AlgorithmMetrics first = perCycleMetrics.get(0);
        assertEquals(0.0, first.getTimestamp(), 0.01,
            "First cycle timestamp should be 0.0");

        // Verify second cycle
        AlgorithmMetrics second = perCycleMetrics.get(1);
        assertEquals(10.0, second.getTimestamp(), 0.01,
            "Second cycle timestamp should be 10.0");

        // Verify third cycle
        AlgorithmMetrics third = perCycleMetrics.get(2);
        assertEquals(20.0, third.getTimestamp(), 0.01,
            "Third cycle timestamp should be 20.0");
    }

    @Test
    public void testConsecutiveOverloadTickReset() {
        // Create 3 consecutive overloads
        for (int i = 0; i < 3; i++) {
            MetricsSnapshot overloadSnapshot = new MetricsSnapshot(
                i, 0.9, 0.8, 5, 0, 0, 10, 0.0
            );
            LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
                .algorithmName("test")
                .timestamp(i)
                .build();

            tracker.recordEvaluation(testPlan, overloadSnapshot, 10L);
        }

        // No overload - should reset counter
        MetricsSnapshot normalSnapshot = new MetricsSnapshot(
            3, 0.5, 0.5, 0, 0, 0, 10, 0.0
        );
        LoadBalancingPlan normalPlan = new LoadBalancingPlan.Builder()
            .algorithmName("test")
            .timestamp(3)
            .build();

        AlgorithmMetrics afterReset = tracker.recordEvaluation(normalPlan, normalSnapshot, 10L);

        assertEquals(0, afterReset.getSlaViolations(),
            "SLA violations should be reset after no overload");

        // Need 5 more consecutive overloads to trigger SLA violation again
        for (int i = 4; i < 9; i++) {
            MetricsSnapshot overloadSnapshot = new MetricsSnapshot(
                i, 0.9, 0.8, 5, 0, 0, 10, 0.0
            );
            LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
                .algorithmName("test")
                .timestamp(i)
                .build();

            AlgorithmMetrics metrics = tracker.recordEvaluation(testPlan, overloadSnapshot, 10L);

            if (i < 8) {
                assertEquals(0, metrics.getSlaViolations(),
                    "No SLA violation until 5 consecutive overloads after reset");
            } else {
                assertEquals(1, metrics.getSlaViolations(),
                    "SLA violation should trigger after 5 consecutive overloads");
            }
        }
    }

    @Test
    public void testPerCycleMetricsListIsolation() {
        // Record some metrics
        tracker.recordEvaluation(plan, snapshot, 10L);

        List<AlgorithmMetrics> list1 = tracker.getPerCycleMetrics();
        assertEquals(1, list1.size(), "Should have 1 metric");

        // Modify returned list (should not affect internal state)
        list1.clear();

        // Get list again
        List<AlgorithmMetrics> list2 = tracker.getPerCycleMetrics();
        assertEquals(1, list2.size(),
            "Internal list should still have 1 metric (isolation)");
    }

    @Test
    public void testMultipleAlgorithmsInSequence() {
        // Record metrics for threshold algorithm
        LoadBalancingPlan thresholdPlan = new LoadBalancingPlan.Builder()
            .algorithmName("threshold")
            .timestamp(10.0)
            .build();

        AlgorithmMetrics thresholdMetrics = tracker.recordEvaluation(
            thresholdPlan, snapshot, 15L
        );

        assertEquals("threshold", thresholdMetrics.getAlgorithmName());

        // Record metrics for nsga2 algorithm
        LoadBalancingPlan nsga2Plan = new LoadBalancingPlan.Builder()
            .algorithmName("nsga2")
            .timestamp(20.0)
            .build();

        AlgorithmMetrics nsga2Metrics = tracker.recordEvaluation(
            nsga2Plan, snapshot, 50L
        );

        assertEquals("nsga2", nsga2Metrics.getAlgorithmName());

        // Verify both are in per-cycle metrics
        List<AlgorithmMetrics> allMetrics = tracker.getPerCycleMetrics();
        assertEquals(2, allMetrics.size(), "Should have metrics for both algorithms");
        assertEquals("threshold", allMetrics.get(0).getAlgorithmName());
        assertEquals("nsga2", allMetrics.get(1).getAlgorithmName());
    }

    @Test
    public void testZeroOverloads() {
        MetricsSnapshot noOverloadSnapshot = new MetricsSnapshot(
            10.0, 0.5, 0.5, 0, 0, 0, 10, 0.0
        );

        LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
            .algorithmName("test")
            .timestamp(10.0)
            .build();

        AlgorithmMetrics metrics = tracker.recordEvaluation(testPlan, noOverloadSnapshot, 10L);

        assertEquals(0, metrics.getOverloadEvents(),
            "Should handle zero overload events");
        assertEquals(0, metrics.getSlaViolations(),
            "Should have no SLA violations with zero overloads");
    }

    @Test
    public void testTimestampProgression() {
        double[] timestamps = {0.0, 10.0, 20.0, 30.0, 40.0};

        for (double timestamp : timestamps) {
            LoadBalancingPlan testPlan = new LoadBalancingPlan.Builder()
                .algorithmName("test")
                .timestamp(timestamp)
                .build();

            MetricsSnapshot testSnapshot = new MetricsSnapshot(
                timestamp, 0.6, 0.5, 1, 0, 0, 10, 0.0
            );

            tracker.recordEvaluation(testPlan, testSnapshot, 10L);
        }

        List<AlgorithmMetrics> metrics = tracker.getPerCycleMetrics();

        assertEquals(5, metrics.size(), "Should have 5 metrics");

        for (int i = 0; i < 5; i++) {
            assertEquals(timestamps[i], metrics.get(i).getTimestamp(), 0.01,
                "Timestamp should progress correctly");
        }
    }
}
