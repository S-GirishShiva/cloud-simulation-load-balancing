package com.cloudsimulation.algorithms;

import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.LoadBalancingPlan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NoOpPolicy class.
 * Tests that the no-op policy behaves correctly as a baseline implementation.
 */
public class NoOpPolicyTest {
    private NoOpPolicy policy;

    @BeforeEach
    public void setup() {
        policy = new NoOpPolicy();
    }

    @Test
    public void testGetName() {
        assertEquals("noop", policy.getName(), "Policy name should be 'noop'");
    }

    @Test
    public void testEvaluateReturnsEmptyPlan() {
        MetricsSnapshot snapshot = new MetricsSnapshot(
            10.0,  // timestamp
            0.5,   // avgCpuUtilization
            0.4,   // avgMemoryUtilization
            5,     // overloadedVmCount
            3,     // underloadedVmCount
            0,     // totalMigrations
            50,    // activeVmCount
            100.0  // powerConsumption
        );

        LoadBalancingPlan plan = policy.evaluate(snapshot);

        assertNotNull(plan, "Plan should not be null");
        assertEquals(0, plan.getMigrationCount(), "Plan should have no migrations");
        assertEquals("noop", plan.getAlgorithmName(), "Algorithm name should be 'noop'");
        assertEquals(10.0, plan.getTimestamp(), "Timestamp should match snapshot");
        assertTrue(plan.getMigrations().isEmpty(), "Migrations list should be empty");
    }

    @Test
    public void testEvaluateWithNullSnapshot() {
        assertThrows(NullPointerException.class, () -> {
            policy.evaluate(null);
        }, "Evaluating with null snapshot should throw NullPointerException");
    }

    @Test
    public void testEvaluateWithHighLoad() {
        // Test with overloaded system - should still return empty plan
        MetricsSnapshot snapshot = new MetricsSnapshot(
            20.0,  // timestamp
            0.95,  // avgCpuUtilization (high)
            0.9,   // avgMemoryUtilization (high)
            20,    // overloadedVmCount (many)
            0,     // underloadedVmCount
            0,     // totalMigrations
            100,   // activeVmCount
            500.0  // powerConsumption
        );

        LoadBalancingPlan plan = policy.evaluate(snapshot);

        assertNotNull(plan, "Plan should not be null even with high load");
        assertEquals(0, plan.getMigrationCount(), "NoOp should not migrate even with high load");
    }

    @Test
    public void testEvaluateWithLowLoad() {
        // Test with underloaded system - should still return empty plan
        MetricsSnapshot snapshot = new MetricsSnapshot(
            30.0,  // timestamp
            0.1,   // avgCpuUtilization (low)
            0.15,  // avgMemoryUtilization (low)
            0,     // overloadedVmCount
            30,    // underloadedVmCount (many)
            0,     // totalMigrations
            100,   // activeVmCount
            200.0  // powerConsumption
        );

        LoadBalancingPlan plan = policy.evaluate(snapshot);

        assertNotNull(plan, "Plan should not be null even with low load");
        assertEquals(0, plan.getMigrationCount(), "NoOp should not migrate even with low load");
    }

    @Test
    public void testReset() {
        // Evaluate a few times to increment counter
        MetricsSnapshot snapshot = new MetricsSnapshot(1.0, 0.5, 0.5, 0, 0, 0, 10, 50.0);

        policy.evaluate(snapshot);
        policy.evaluate(snapshot);
        policy.evaluate(snapshot);

        assertEquals(3, policy.getEvaluationCount(), "Evaluation count should be 3");

        // Reset should clear counter
        policy.reset();

        assertEquals(0, policy.getEvaluationCount(), "Evaluation count should be 0 after reset");
    }

    @Test
    public void testEvaluationCounter() {
        MetricsSnapshot snapshot = new MetricsSnapshot(1.0, 0.5, 0.5, 0, 0, 0, 10, 50.0);

        assertEquals(0, policy.getEvaluationCount(), "Initial count should be 0");

        policy.evaluate(snapshot);
        assertEquals(1, policy.getEvaluationCount(), "Count should be 1 after first evaluation");

        policy.evaluate(snapshot);
        assertEquals(2, policy.getEvaluationCount(), "Count should be 2 after second evaluation");

        policy.evaluate(snapshot);
        assertEquals(3, policy.getEvaluationCount(), "Count should be 3 after third evaluation");
    }

    @Test
    public void testMultipleResets() {
        MetricsSnapshot snapshot = new MetricsSnapshot(1.0, 0.5, 0.5, 0, 0, 0, 10, 50.0);

        policy.evaluate(snapshot);
        policy.reset();
        assertEquals(0, policy.getEvaluationCount(), "Count should be 0 after first reset");

        policy.evaluate(snapshot);
        policy.evaluate(snapshot);
        policy.reset();
        assertEquals(0, policy.getEvaluationCount(), "Count should be 0 after second reset");
    }

    @Test
    public void testComputationTime() {
        MetricsSnapshot snapshot = new MetricsSnapshot(1.0, 0.5, 0.5, 0, 0, 0, 10, 50.0);
        LoadBalancingPlan plan = policy.evaluate(snapshot);

        assertEquals(0L, plan.getComputationTime(), "NoOp computation time should be 0");
    }

    @Test
    public void testImmutablePlan() {
        MetricsSnapshot snapshot = new MetricsSnapshot(1.0, 0.5, 0.5, 0, 0, 0, 10, 50.0);
        LoadBalancingPlan plan = policy.evaluate(snapshot);

        // Verify that migrations list is immutable
        assertThrows(UnsupportedOperationException.class, () -> {
            plan.getMigrations().add(null);
        }, "Migrations list should be immutable");
    }
}
