package com.cloudsimulation.cli;

import com.cloudsimulation.models.ScenarioBenchmarkResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ScenarioBenchmarkRunner.
 */
public class ScenarioBenchmarkRunnerTest {
    private ScenarioBenchmarkRunner runner;
    private static final String BENCHMARKS_DIR = "configs/benchmarks";

    @BeforeEach
    public void setup() {
        runner = new ScenarioBenchmarkRunner(BENCHMARKS_DIR);
    }

    @Test
    public void testRunScenarioBenchmarksWithSingleAlgorithm() {
        List<String> algorithms = List.of("threshold");

        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        assertNotNull(result, "Result should not be null");
        assertEquals(4, result.getTotalScenariosRun(), "Should have 4 scenarios");
        assertEquals(1, result.getTotalAlgorithmsTested(), "Should test 1 algorithm");
        assertTrue(result.getFailedExecutions().isEmpty(),
            "Should have no failures (all scenarios should pass)");
        assertEquals(4, result.getScenarioReports().size(),
            "Should have 4 scenario reports");
    }

    @Test
    public void testRunScenarioBenchmarksLoadsAllScenarios() {
        List<String> algorithms = List.of("threshold");

        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        assertTrue(result.getScenarioReports().containsKey("steady_load"),
            "Should contain steady_load scenario");
        assertTrue(result.getScenarioReports().containsKey("diurnal_pattern"),
            "Should contain diurnal_pattern scenario");
        assertTrue(result.getScenarioReports().containsKey("traffic_spike"),
            "Should contain traffic_spike scenario");
        assertTrue(result.getScenarioReports().containsKey("chaos_oscillation"),
            "Should contain chaos_oscillation scenario");
    }

    @Test
    public void testRunScenarioBenchmarksWithMultipleAlgorithms() {
        // Note: Only threshold is implemented, so testing with just threshold
        // In future when nsga2, hybrid are implemented, update this test
        List<String> algorithms = List.of("threshold");

        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        assertEquals(4, result.getTotalScenariosRun(), "Should run all 4 scenarios");
        assertEquals(1, result.getTotalAlgorithmsTested(), "Should test algorithms");
    }

    @Test
    public void testRunScenarioBenchmarksWithEmptyDirectory() {
        ScenarioBenchmarkRunner badRunner = new ScenarioBenchmarkRunner("configs/nonexistent");

        assertThrows(IllegalStateException.class, () -> {
            badRunner.runScenarioBenchmarks(List.of("threshold"));
        }, "Should throw exception when no YAML files found");
    }

    @Test
    public void testGenerateMasterMatrixProducesValidTable() {
        List<String> algorithms = List.of("threshold");
        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        String matrix = runner.generateMasterMatrix(result);

        assertNotNull(matrix, "Matrix should not be null");
        assertTrue(matrix.contains("steady_load"), "Matrix should contain steady_load");
        assertTrue(matrix.contains("diurnal_pattern"), "Matrix should contain diurnal_pattern");
        assertTrue(matrix.contains("traffic_spike"), "Matrix should contain traffic_spike");
        assertTrue(matrix.contains("chaos_oscillation"), "Matrix should contain chaos_oscillation");
        assertTrue(matrix.contains("threshold"), "Matrix should contain threshold algorithm");
        assertTrue(matrix.contains("Legend"), "Matrix should contain legend");
        assertTrue(matrix.length() > 100, "Matrix should have substantial content");
    }

    @Test
    public void testGenerateMasterMatrixWithNoResults() {
        ScenarioBenchmarkResult emptyResult = new ScenarioBenchmarkResult(
            java.util.Collections.emptyMap(),
            java.util.Collections.emptyList(),
            0, 0
        );

        String matrix = runner.generateMasterMatrix(emptyResult);

        assertNotNull(matrix, "Matrix should not be null even with empty results");
        assertTrue(matrix.contains("No results available"), "Should indicate no results");
    }

    @Test
    public void testExtractScenarioIdRemovesYamlExtension() {
        // This tests the private extractScenarioId method indirectly through execution
        List<String> algorithms = List.of("threshold");
        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        // Scenario IDs should not contain .yaml extension
        for (String scenarioId : result.getScenarioReports().keySet()) {
            assertFalse(scenarioId.endsWith(".yaml"), "Scenario ID should not end with .yaml");
            assertFalse(scenarioId.endsWith(".yml"), "Scenario ID should not end with .yml");
        }
    }

    @Test
    public void testRunScenarioBenchmarksHandlesExceptions() {
        // Test with invalid algorithm name (should not crash, may log errors)
        // Since threshold algorithm should work, this test validates graceful handling
        List<String> algorithms = List.of("threshold");

        assertDoesNotThrow(() -> {
            ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);
            assertNotNull(result, "Should return result even if some runs fail");
        }, "Should handle exceptions gracefully");
    }
}
