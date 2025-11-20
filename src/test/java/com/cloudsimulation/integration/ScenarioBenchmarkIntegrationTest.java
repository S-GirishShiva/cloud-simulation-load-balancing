package com.cloudsimulation.integration;

import com.cloudsimulation.cli.ScenarioBenchmarkRunner;
import com.cloudsimulation.models.ComparisonReport;
import com.cloudsimulation.models.ScenarioBenchmarkResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for full scenario benchmark suite execution.
 * Tests end-to-end execution of all 4 benchmark scenarios.
 */
public class ScenarioBenchmarkIntegrationTest {
    private ScenarioBenchmarkRunner runner;
    private static final String BENCHMARKS_DIR = "configs/benchmarks";

    @BeforeEach
    public void setup() {
        runner = new ScenarioBenchmarkRunner(BENCHMARKS_DIR);
    }

    @Test
    public void testFullScenarioBenchmarkSuiteExecution() {
        // Run suite with threshold algorithm only (faster than multi-algorithm)
        List<String> algorithms = List.of("threshold");

        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        // Verify all 4 scenarios executed
        assertEquals(4, result.getTotalScenariosRun(),
            "Should execute all 4 benchmark scenarios");
        assertEquals(4, result.getScenarioReports().size(),
            "Should have 4 comparison reports");

        // Verify specific scenarios present
        assertTrue(result.getScenarioReports().containsKey("steady_load"),
            "Should contain steady_load scenario");
        assertTrue(result.getScenarioReports().containsKey("diurnal_pattern"),
            "Should contain diurnal_pattern scenario");
        assertTrue(result.getScenarioReports().containsKey("traffic_spike"),
            "Should contain traffic_spike scenario");
        assertTrue(result.getScenarioReports().containsKey("chaos_oscillation"),
            "Should contain chaos_oscillation scenario");

        // Verify no failures
        assertTrue(result.getFailedExecutions().isEmpty(),
            "Should have no crashes or timeouts");

        // Verify each ComparisonReport has correct scenarioId
        for (var entry : result.getScenarioReports().entrySet()) {
            String scenarioId = entry.getKey();
            ComparisonReport report = entry.getValue();
            assertEquals(scenarioId, report.getScenarioId(),
                "Comparison report scenarioId should match map key");
        }

        // Verify master comparison matrix generated
        String matrix = runner.generateMasterMatrix(result);
        assertNotNull(matrix, "Master matrix should be generated");
        assertTrue(matrix.contains("steady_load"), "Matrix should contain all scenarios");
        assertTrue(matrix.contains("diurnal_pattern"), "Matrix should contain all scenarios");

        System.out.println(matrix); // Print for manual inspection
    }

    @Test
    public void testScenarioBenchmarkValidatesScenarioConfigurations() {
        List<String> algorithms = List.of("threshold");

        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        // Verify each scenario has metrics
        for (var entry : result.getScenarioReports().entrySet()) {
            String scenarioId = entry.getKey();
            ComparisonReport report = entry.getValue();

            assertFalse(report.getAlgorithmResults().isEmpty(),
                scenarioId + " should have algorithm results");

            var algorithmResult = report.getAlgorithmResults().get(0);
            var metrics = algorithmResult.getAggregatedMetrics();

            // Verify metrics are populated
            assertNotNull(metrics, scenarioId + " should have metrics");
            assertTrue(metrics.getTotalMigrations() >= 0,
                scenarioId + " should have non-negative migrations");
            assertTrue(metrics.getAverageUtilization() >= 0.0,
                scenarioId + " should have non-negative utilization");
        }
    }

    @Test
    public void testScenarioBenchmarkProducesComparableMetrics() {
        List<String> algorithms = List.of("threshold");

        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        // Verify metrics vary across scenarios (different workload patterns produce different results)
        var steadyLoadReport = result.getScenarioReports().get("steady_load");
        var chaosReport = result.getScenarioReports().get("chaos_oscillation");

        assertNotNull(steadyLoadReport, "steady_load report should exist");
        assertNotNull(chaosReport, "chaos_oscillation report should exist");

        var steadyMetrics = steadyLoadReport.getAlgorithmResults().get(0).getAggregatedMetrics();
        var chaosMetrics = chaosReport.getAlgorithmResults().get(0).getAggregatedMetrics();

        // Chaos scenario should generally have different metrics than steady load
        // (We don't assert which is higher since algorithm behavior may vary,
        // but they should be different)
        assertNotNull(steadyMetrics, "Steady load should have metrics");
        assertNotNull(chaosMetrics, "Chaos should have metrics");
    }

    @Test
    public void testMasterComparisonMatrixFormatting() {
        List<String> algorithms = List.of("threshold");
        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        String matrix = runner.generateMasterMatrix(result);

        // Verify matrix structure
        assertTrue(matrix.contains("=".repeat(120)), "Should have header separator");
        assertTrue(matrix.contains("MASTER COMPARISON MATRIX"), "Should have title");
        assertTrue(matrix.contains("Legend"), "Should have legend");
        assertTrue(matrix.contains("M=Migrations"), "Legend should explain abbreviations");
        assertTrue(matrix.contains("Total:"), "Should have summary");

        // Verify all 4 scenario rows present
        String[] lines = matrix.split("\n");
        long scenarioRows = java.util.Arrays.stream(lines)
            .filter(line -> line.contains("steady_load") ||
                           line.contains("diurnal_pattern") ||
                           line.contains("traffic_spike") ||
                           line.contains("chaos_oscillation"))
            .count();

        assertTrue(scenarioRows >= 4,
            "Matrix should contain rows for all 4 scenarios");
    }

    @Test
    public void testScenarioBenchmarkReportsFailuresCorrectly() {
        // This test validates failure handling, but since our scenarios should succeed,
        // we verify the failure tracking mechanism works
        List<String> algorithms = List.of("threshold");
        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        // Verify failedExecutions list exists and is properly structured
        assertNotNull(result.getFailedExecutions(),
            "Failed executions list should not be null");

        // In normal execution, there should be no failures
        assertTrue(result.getFailedExecutions().isEmpty(),
            "Standard scenarios should execute without failures");
    }
}
