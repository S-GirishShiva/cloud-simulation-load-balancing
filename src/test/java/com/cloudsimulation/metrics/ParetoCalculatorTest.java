package com.cloudsimulation.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ParetoCalculator class.
 * Tests Pareto dominance logic across 2D, 3D, and 4D objective spaces.
 */
public class ParetoCalculatorTest {
    private ParetoCalculator calculator;

    @BeforeEach
    public void setup() {
        calculator = new ParetoCalculator();
    }

    @Test
    public void testDominatesStrictlyBetter() {
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(2)
            .slaViolations(0)
            .averageUtilization(0.8)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(10)
            .overloadEvents(5)
            .slaViolations(1)
            .averageUtilization(0.6)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B (better in all objectives)");
        assertFalse(calculator.dominates(b, a),
            "B should not dominate A");
    }

    @Test
    public void testDominatesEqual() {
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(3)
            .slaViolations(1)
            .averageUtilization(0.75)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(3)
            .slaViolations(1)
            .averageUtilization(0.75)
            .build();

        assertFalse(calculator.dominates(a, b),
            "A should not dominate B (equal in all objectives)");
        assertFalse(calculator.dominates(b, a),
            "B should not dominate A (equal in all objectives)");
    }

    @Test
    public void testDominatesWorse() {
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(15)
            .overloadEvents(10)
            .slaViolations(5)
            .averageUtilization(0.4)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(2)
            .slaViolations(0)
            .averageUtilization(0.8)
            .build();

        assertFalse(calculator.dominates(a, b),
            "A should not dominate B (worse in all objectives)");
        assertTrue(calculator.dominates(b, a),
            "B should dominate A (better in all objectives)");
    }

    @Test
    public void testDominatesMixedObjectives() {
        // A is better in migrations, worse in utilization
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(3)
            .slaViolations(1)
            .averageUtilization(0.6)
            .build();

        // B is worse in migrations, better in utilization
        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(10)
            .overloadEvents(3)
            .slaViolations(1)
            .averageUtilization(0.8)
            .build();

        assertFalse(calculator.dominates(a, b),
            "A should not dominate B (trade-offs exist)");
        assertFalse(calculator.dominates(b, a),
            "B should not dominate A (trade-offs exist)");
    }

    @Test
    public void testDominates2DObjectiveSpace() {
        // Test with only migrations and utilization
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .averageUtilization(0.8)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(10)
            .averageUtilization(0.6)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B in 2D space (fewer migrations, higher utilization)");
    }

    @Test
    public void testDominates3DObjectiveSpace() {
        // Test with migrations, overloads, and utilization
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(2)
            .averageUtilization(0.8)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(10)
            .overloadEvents(5)
            .averageUtilization(0.6)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B in 3D space");
    }

    @Test
    public void testDominates4DObjectiveSpace() {
        // Test with all four objectives
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(2)
            .slaViolations(0)
            .averageUtilization(0.8)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(10)
            .overloadEvents(5)
            .slaViolations(2)
            .averageUtilization(0.6)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B in 4D space");
    }

    @Test
    public void testFindNonDominatedSolutionsSingleSolution() {
        AlgorithmMetrics single = new AlgorithmMetrics.Builder()
            .algorithmName("only")
            .totalMigrations(5)
            .build();

        List<AlgorithmMetrics> all = List.of(single);
        List<AlgorithmMetrics> paretoFront = calculator.findNonDominatedSolutions(all);

        assertEquals(1, paretoFront.size(),
            "Single solution should be in Pareto front");
        assertTrue(paretoFront.contains(single),
            "Pareto front should contain the only solution");
    }

    @Test
    public void testFindNonDominatedSolutionsAllDominated() {
        // Create scenario where one solution dominates all others
        AlgorithmMetrics best = new AlgorithmMetrics.Builder()
            .algorithmName("best")
            .totalMigrations(5)
            .overloadEvents(2)
            .slaViolations(0)
            .averageUtilization(0.9)
            .build();

        AlgorithmMetrics worse1 = new AlgorithmMetrics.Builder()
            .algorithmName("worse1")
            .totalMigrations(10)
            .overloadEvents(5)
            .slaViolations(2)
            .averageUtilization(0.7)
            .build();

        AlgorithmMetrics worse2 = new AlgorithmMetrics.Builder()
            .algorithmName("worse2")
            .totalMigrations(15)
            .overloadEvents(8)
            .slaViolations(3)
            .averageUtilization(0.5)
            .build();

        List<AlgorithmMetrics> all = List.of(best, worse1, worse2);
        List<AlgorithmMetrics> paretoFront = calculator.findNonDominatedSolutions(all);

        assertEquals(1, paretoFront.size(),
            "Only best solution should be in Pareto front");
        assertTrue(paretoFront.contains(best),
            "Pareto front should contain best solution");
        assertFalse(paretoFront.contains(worse1),
            "Dominated solution should not be in Pareto front");
        assertFalse(paretoFront.contains(worse2),
            "Dominated solution should not be in Pareto front");
    }

    @Test
    public void testFindNonDominatedSolutionsAllNonDominated() {
        // Create scenario with trade-offs - no solution dominates another
        AlgorithmMetrics m1 = new AlgorithmMetrics.Builder()
            .algorithmName("low-migrations")
            .totalMigrations(5)
            .overloadEvents(10)
            .slaViolations(2)
            .averageUtilization(0.5)
            .build();

        AlgorithmMetrics m2 = new AlgorithmMetrics.Builder()
            .algorithmName("low-overloads")
            .totalMigrations(10)
            .overloadEvents(2)
            .slaViolations(3)
            .averageUtilization(0.6)
            .build();

        AlgorithmMetrics m3 = new AlgorithmMetrics.Builder()
            .algorithmName("high-utilization")
            .totalMigrations(15)
            .overloadEvents(8)
            .slaViolations(1)
            .averageUtilization(0.9)
            .build();

        List<AlgorithmMetrics> all = List.of(m1, m2, m3);
        List<AlgorithmMetrics> paretoFront = calculator.findNonDominatedSolutions(all);

        assertEquals(3, paretoFront.size(),
            "All solutions should be in Pareto front (trade-offs)");
        assertTrue(paretoFront.contains(m1));
        assertTrue(paretoFront.contains(m2));
        assertTrue(paretoFront.contains(m3));
    }

    @Test
    public void testFindNonDominatedSolutionsKnownParetoFront() {
        // Known Pareto front: m1 and m2 are non-dominated, m3 is dominated by m1
        AlgorithmMetrics m1 = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .totalMigrations(10)
            .overloadEvents(5)
            .slaViolations(1)
            .averageUtilization(0.5)
            .build();

        AlgorithmMetrics m2 = new AlgorithmMetrics.Builder()
            .algorithmName("nsga2")
            .totalMigrations(5)
            .overloadEvents(10)
            .slaViolations(0)
            .averageUtilization(0.7)
            .build();

        AlgorithmMetrics m3 = new AlgorithmMetrics.Builder()
            .algorithmName("dominated")
            .totalMigrations(12)
            .overloadEvents(6)
            .slaViolations(2)
            .averageUtilization(0.4)
            .build();

        List<AlgorithmMetrics> all = List.of(m1, m2, m3);
        List<AlgorithmMetrics> paretoFront = calculator.findNonDominatedSolutions(all);

        assertEquals(2, paretoFront.size(),
            "Pareto front should have 2 solutions");
        assertTrue(paretoFront.contains(m1),
            "m1 should be in Pareto front");
        assertTrue(paretoFront.contains(m2),
            "m2 should be in Pareto front");
        assertFalse(paretoFront.contains(m3),
            "m3 should be dominated and not in Pareto front");
    }

    @Test
    public void testFindNonDominatedSolutionsEmptyList() {
        List<AlgorithmMetrics> empty = new ArrayList<>();
        List<AlgorithmMetrics> paretoFront = calculator.findNonDominatedSolutions(empty);

        assertEquals(0, paretoFront.size(),
            "Empty list should produce empty Pareto front");
    }

    @Test
    public void testDominatesPartiallyBetter() {
        // A is better in migrations, equal in others
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(3)
            .slaViolations(1)
            .averageUtilization(0.75)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(10)
            .overloadEvents(3)
            .slaViolations(1)
            .averageUtilization(0.75)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B (better in one objective, equal in others)");
        assertFalse(calculator.dominates(b, a),
            "B should not dominate A");
    }

    @Test
    public void testDominatesUtilizationObjective() {
        // Test maximization objective (utilization)
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .averageUtilization(0.9)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .averageUtilization(0.7)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B (same migrations, higher utilization)");
    }

    @Test
    public void testDominatesZeroValues() {
        AlgorithmMetrics a = new AlgorithmMetrics.Builder()
            .totalMigrations(0)
            .overloadEvents(0)
            .slaViolations(0)
            .averageUtilization(0.8)
            .build();

        AlgorithmMetrics b = new AlgorithmMetrics.Builder()
            .totalMigrations(5)
            .overloadEvents(2)
            .slaViolations(1)
            .averageUtilization(0.6)
            .build();

        assertTrue(calculator.dominates(a, b),
            "A should dominate B (zero is better for minimization objectives)");
    }
}
