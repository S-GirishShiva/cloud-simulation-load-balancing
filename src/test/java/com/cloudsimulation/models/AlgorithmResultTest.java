package com.cloudsimulation.models;

import com.cloudsimulation.metrics.AlgorithmMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AlgorithmResult.
 * Tests percentage improvement calculations over baseline.
 */
public class AlgorithmResultTest {

    @Test
    public void testCalculateImprovementOverBaselineWithBetterCandidate() {
        AlgorithmMetrics baseline = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .totalMigrations(100)
            .overloadEvents(20)
            .averageUtilization(0.60)
            .slaViolations(10)
            .decisionTimeMs(15)
            .build();

        AlgorithmMetrics candidate = new AlgorithmMetrics.Builder()
            .algorithmName("nsga2")
            .totalMigrations(70)  // 30% improvement
            .overloadEvents(15)   // 25% improvement
            .averageUtilization(0.72)  // 20% improvement
            .slaViolations(5)     // 50% improvement
            .decisionTimeMs(10)   // 33.3% improvement
            .build();

        AlgorithmResult result = new AlgorithmResult("nsga2", candidate, false, true);
        result.calculateImprovementOverBaseline(baseline);

        assertEquals(30.0, result.getPercentageImprovement().get("migrations"), 0.1,
            "Migrations improvement should be 30%");
        assertEquals(25.0, result.getPercentageImprovement().get("overloads"), 0.1,
            "Overloads improvement should be 25%");
        assertEquals(20.0, result.getPercentageImprovement().get("utilization"), 0.1,
            "Utilization improvement should be 20%");
        assertEquals(50.0, result.getPercentageImprovement().get("sla_violations"), 0.1,
            "SLA violations improvement should be 50%");
        assertEquals(33.33, result.getPercentageImprovement().get("decision_time"), 0.1,
            "Decision time improvement should be 33.3%");
    }

    @Test
    public void testCalculateImprovementOverBaselineWithWorseCandidate() {
        AlgorithmMetrics baseline = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .totalMigrations(100)
            .overloadEvents(20)
            .averageUtilization(0.60)
            .slaViolations(10)
            .decisionTimeMs(15)
            .build();

        AlgorithmMetrics candidate = new AlgorithmMetrics.Builder()
            .algorithmName("slow")
            .totalMigrations(150)  // -50% (worse)
            .overloadEvents(30)    // -50% (worse)
            .averageUtilization(0.50)  // -16.67% (worse)
            .slaViolations(15)     // -50% (worse)
            .decisionTimeMs(30)    // -100% (worse)
            .build();

        AlgorithmResult result = new AlgorithmResult("slow", candidate, false, false);
        result.calculateImprovementOverBaseline(baseline);

        assertEquals(-50.0, result.getPercentageImprovement().get("migrations"), 0.1,
            "Negative improvement for worse migrations");
        assertEquals(-50.0, result.getPercentageImprovement().get("overloads"), 0.1,
            "Negative improvement for worse overloads");
        assertEquals(-16.67, result.getPercentageImprovement().get("utilization"), 0.1,
            "Negative improvement for worse utilization");
        assertEquals(-50.0, result.getPercentageImprovement().get("sla_violations"), 0.1,
            "Negative improvement for worse SLA violations");
        assertEquals(-100.0, result.getPercentageImprovement().get("decision_time"), 0.1,
            "Negative improvement for worse decision time");
    }

    @Test
    public void testCalculateImprovementHandlesZeroBaseline() {
        AlgorithmMetrics baseline = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .totalMigrations(0)  // Zero baseline
            .overloadEvents(0)
            .averageUtilization(0.0)
            .slaViolations(0)
            .decisionTimeMs(0)
            .build();

        AlgorithmMetrics candidate = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .totalMigrations(10)
            .overloadEvents(5)
            .averageUtilization(0.5)
            .slaViolations(2)
            .decisionTimeMs(5)
            .build();

        AlgorithmResult result = new AlgorithmResult("test", candidate, false, false);
        result.calculateImprovementOverBaseline(baseline);

        assertEquals(0.0, result.getPercentageImprovement().get("migrations"),
            "Should handle zero baseline gracefully");
        assertEquals(0.0, result.getPercentageImprovement().get("overloads"),
            "Should handle zero baseline gracefully");
        assertEquals(0.0, result.getPercentageImprovement().get("utilization"),
            "Should handle zero baseline gracefully");
        assertEquals(0.0, result.getPercentageImprovement().get("sla_violations"),
            "Should handle zero baseline gracefully");
        assertEquals(0.0, result.getPercentageImprovement().get("decision_time"),
            "Should handle zero baseline gracefully");
    }

    @Test
    public void testBaselineAlgorithmHasZeroImprovement() {
        AlgorithmMetrics baseline = new AlgorithmMetrics.Builder()
            .algorithmName("threshold")
            .totalMigrations(100)
            .overloadEvents(20)
            .averageUtilization(0.60)
            .slaViolations(10)
            .decisionTimeMs(15)
            .build();

        AlgorithmResult result = new AlgorithmResult("threshold", baseline, true, false);
        result.calculateImprovementOverBaseline(baseline);

        assertEquals(0.0, result.getPercentageImprovement().get("migrations"),
            "Baseline has 0% improvement over itself");
        assertEquals(0.0, result.getPercentageImprovement().get("overloads"),
            "Baseline has 0% improvement over itself");
        assertEquals(0.0, result.getPercentageImprovement().get("utilization"),
            "Baseline has 0% improvement over itself");
        assertEquals(0.0, result.getPercentageImprovement().get("sla_violations"),
            "Baseline has 0% improvement over itself");
        assertEquals(0.0, result.getPercentageImprovement().get("decision_time"),
            "Baseline has 0% improvement over itself");
    }

    @Test
    public void testMinimizationObjectiveImprovement() {
        // For minimization: improvement = (baseline - candidate) / baseline * 100
        AlgorithmMetrics baseline = new AlgorithmMetrics.Builder()
            .algorithmName("baseline")
            .totalMigrations(200)
            .build();

        AlgorithmMetrics candidate = new AlgorithmMetrics.Builder()
            .algorithmName("candidate")
            .totalMigrations(150)  // 25% reduction = 25% improvement
            .build();

        AlgorithmResult result = new AlgorithmResult("candidate", candidate, false, false);
        result.calculateImprovementOverBaseline(baseline);

        assertEquals(25.0, result.getPercentageImprovement().get("migrations"), 0.1,
            "25% reduction in migrations should be 25% improvement");
    }

    @Test
    public void testMaximizationObjectiveImprovement() {
        // For maximization: improvement = (candidate - baseline) / baseline * 100
        AlgorithmMetrics baseline = new AlgorithmMetrics.Builder()
            .algorithmName("baseline")
            .averageUtilization(0.50)
            .build();

        AlgorithmMetrics candidate = new AlgorithmMetrics.Builder()
            .algorithmName("candidate")
            .averageUtilization(0.60)  // 20% increase = 20% improvement
            .build();

        AlgorithmResult result = new AlgorithmResult("candidate", candidate, false, false);
        result.calculateImprovementOverBaseline(baseline);

        assertEquals(20.0, result.getPercentageImprovement().get("utilization"), 0.1,
            "20% increase in utilization should be 20% improvement");
    }

    @Test
    public void testGetters() {
        AlgorithmMetrics metrics = new AlgorithmMetrics.Builder()
            .algorithmName("test")
            .totalMigrations(100)
            .build();

        AlgorithmResult result = new AlgorithmResult("test", metrics, false, true);

        assertEquals("test", result.getAlgorithmName());
        assertEquals(metrics, result.getAggregatedMetrics());
        assertFalse(result.isBaselineAlgorithm());
        assertTrue(result.isParetoOptimal());
        assertNotNull(result.getPercentageImprovement());
    }
}
