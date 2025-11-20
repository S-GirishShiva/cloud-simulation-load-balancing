package com.cloudsimulation.models;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LoadBalancingPlan class.
 * Tests Builder pattern, immutability, and multiple migrations.
 */
public class LoadBalancingPlanTest {

    @Test
    public void testBuilderWithDefaults() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder().build();

        assertNotNull(plan, "Plan should not be null");
        assertNotNull(plan.getDecisionId(), "Decision ID should have default value");
        assertEquals(0.0, plan.getTimestamp(), 0.001, "Default timestamp should be 0.0");
        assertEquals("unknown", plan.getAlgorithmName(), "Default algorithm name should be 'unknown'");
        assertNotNull(plan.getMigrations(), "Migrations list should not be null");
        assertTrue(plan.getMigrations().isEmpty(), "Default migrations should be empty");
        assertEquals(0L, plan.getComputationTime(), "Default computation time should be 0");
        assertEquals(0, plan.getMigrationCount(), "Default migration count should be 0");
    }

    @Test
    public void testBuilderWithAllFields() {
        MigrationAction action1 = new MigrationAction(1, 2, 3, 1.0, "test1");
        MigrationAction action2 = new MigrationAction(4, 5, 6, 2.0, "test2");
        List<MigrationAction> migrations = List.of(action1, action2);

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .decisionId("test-decision-123")
            .timestamp(42.5)
            .algorithmName("threshold")
            .migrations(migrations)
            .computationTime(150L)
            .build();

        assertEquals("test-decision-123", plan.getDecisionId(), "Decision ID should match");
        assertEquals(42.5, plan.getTimestamp(), 0.001, "Timestamp should match");
        assertEquals("threshold", plan.getAlgorithmName(), "Algorithm name should match");
        assertEquals(2, plan.getMigrationCount(), "Should have 2 migrations");
        assertEquals(150L, plan.getComputationTime(), "Computation time should match");
    }

    @Test
    public void testBuilderAddSingleMigration() {
        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, "test");

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .addMigration(action)
            .build();

        assertEquals(1, plan.getMigrationCount(), "Should have 1 migration");
        assertEquals(action, plan.getMigrations().get(0), "Migration should match");
    }

    @Test
    public void testBuilderAddMultipleMigrations() {
        MigrationAction action1 = new MigrationAction(1, 2, 3, 1.0, "test1");
        MigrationAction action2 = new MigrationAction(4, 5, 6, 2.0, "test2");
        MigrationAction action3 = new MigrationAction(7, 8, 9, 3.0, "test3");

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .addMigration(action1)
            .addMigration(action2)
            .addMigration(action3)
            .build();

        assertEquals(3, plan.getMigrationCount(), "Should have 3 migrations");
        assertTrue(plan.getMigrations().contains(action1), "Should contain action1");
        assertTrue(plan.getMigrations().contains(action2), "Should contain action2");
        assertTrue(plan.getMigrations().contains(action3), "Should contain action3");
    }

    @Test
    public void testMigrationsListImmutable() {
        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, "test");

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .addMigration(action)
            .build();

        // Attempt to modify the returned list should fail
        assertThrows(UnsupportedOperationException.class, () -> {
            plan.getMigrations().add(new MigrationAction(4, 5, 6, 2.0, "illegal"));
        }, "Migrations list should be immutable");
    }

    @Test
    public void testMigrationsListIndependentFromBuilder() {
        List<MigrationAction> originalList = new ArrayList<>();
        originalList.add(new MigrationAction(1, 2, 3, 1.0, "test"));

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .migrations(originalList)
            .build();

        // Modifying original list should not affect plan
        originalList.add(new MigrationAction(4, 5, 6, 2.0, "new"));

        assertEquals(1, plan.getMigrationCount(), "Plan should still have 1 migration");
    }

    @Test
    public void testEmptyPlan() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .algorithmName("noop")
            .build();

        assertTrue(plan.getMigrations().isEmpty(), "Empty plan should have no migrations");
        assertEquals(0, plan.getMigrationCount(), "Migration count should be 0");
    }

    @Test
    public void testToString() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .decisionId("test-123")
            .timestamp(10.0)
            .algorithmName("nsga2")
            .computationTime(200L)
            .build();

        String result = plan.toString();

        assertNotNull(result, "toString should not return null");
        assertTrue(result.contains("test-123"), "toString should contain decision ID");
        assertTrue(result.contains("nsga2"), "toString should contain algorithm name");
    }

    @Test
    public void testLargeMigrationList() {
        LoadBalancingPlan.Builder builder = new LoadBalancingPlan.Builder();

        // Add 100 migrations
        for (int i = 0; i < 100; i++) {
            builder.addMigration(new MigrationAction(i, i, i + 1, 1.0, "bulk-" + i));
        }

        LoadBalancingPlan plan = builder.build();

        assertEquals(100, plan.getMigrationCount(), "Should have 100 migrations");
        assertEquals(100, plan.getMigrations().size(), "Migrations list size should be 100");
    }

    @Test
    public void testBuilderReuse() {
        LoadBalancingPlan.Builder builder = new LoadBalancingPlan.Builder()
            .algorithmName("test")
            .timestamp(1.0);

        LoadBalancingPlan plan1 = builder
            .addMigration(new MigrationAction(1, 2, 3, 1.0, "first"))
            .build();

        LoadBalancingPlan plan2 = builder
            .addMigration(new MigrationAction(4, 5, 6, 2.0, "second"))
            .build();

        // Both plans should exist independently
        assertNotNull(plan1, "First plan should not be null");
        assertNotNull(plan2, "Second plan should not be null");

        // Plans may share some migrations if builder was reused (builder behavior)
        // This test just verifies both plans are created successfully
    }

    @Test
    public void testNullAlgorithmName() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .algorithmName(null)
            .build();

        // Null algorithm name should be allowed (builder sets default if null before this)
        assertNotNull(plan, "Plan should be created even with null algorithm name in builder");
    }

    @Test
    public void testNegativeComputationTime() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .computationTime(-100L)
            .build();

        assertEquals(-100L, plan.getComputationTime(), "Negative computation time should be stored");
    }

    @Test
    public void testZeroComputationTime() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .computationTime(0L)
            .build();

        assertEquals(0L, plan.getComputationTime(), "Zero computation time should be allowed");
    }

    @Test
    public void testNegativeTimestamp() {
        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .timestamp(-5.0)
            .build();

        assertEquals(-5.0, plan.getTimestamp(), 0.001, "Negative timestamp should be allowed");
    }

    @Test
    public void testGetMigrationsReturnsUnmodifiableList() {
        MigrationAction action = new MigrationAction(1, 2, 3, 1.0, "test");

        LoadBalancingPlan plan = new LoadBalancingPlan.Builder()
            .addMigration(action)
            .build();

        List<MigrationAction> migrations = plan.getMigrations();

        assertThrows(UnsupportedOperationException.class, () -> {
            migrations.clear();
        }, "Returned migrations list should be unmodifiable");
    }

    @Test
    public void testMultipleInstancesIndependent() {
        LoadBalancingPlan plan1 = new LoadBalancingPlan.Builder()
            .algorithmName("policy1")
            .timestamp(1.0)
            .build();

        LoadBalancingPlan plan2 = new LoadBalancingPlan.Builder()
            .algorithmName("policy2")
            .timestamp(2.0)
            .build();

        assertNotEquals(plan1.getDecisionId(), plan2.getDecisionId(), "Different plans should have different decision IDs");
        assertNotEquals(plan1.getAlgorithmName(), plan2.getAlgorithmName(), "Different plans should have different algorithm names");
        assertNotEquals(plan1.getTimestamp(), plan2.getTimestamp(), "Different plans should have different timestamps");
    }
}
