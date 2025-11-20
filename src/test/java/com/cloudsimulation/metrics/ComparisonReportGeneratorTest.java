package com.cloudsimulation.metrics;

import com.cloudsimulation.models.AlgorithmResult;
import com.cloudsimulation.models.ComparisonReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ComparisonReportGenerator.
 * Tests report generation, console formatting, and CSV export.
 */
public class ComparisonReportGeneratorTest {
    private ComparisonReportGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        generator = new ComparisonReportGenerator();
    }

    @Test
    public void testGenerateReportWithSingleAlgorithm() {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        runsPerAlgorithm.put("threshold", List.of(createMetrics("threshold", 100, 20, 0.60)));

        ComparisonReport report = generator.generateReport("test_scenario", runsPerAlgorithm);

        assertNotNull(report);
        assertEquals("test_scenario", report.getScenarioId());
        assertEquals(1, report.getAlgorithmResults().size());
        assertEquals("threshold", report.getBaselineAlgorithm());
        assertTrue(report.getParetoFront().contains("threshold"),
            "Single algorithm should be Pareto-optimal");
    }

    @Test
    public void testGenerateReportWithMultipleAlgorithms() {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        runsPerAlgorithm.put("threshold", List.of(createMetrics("threshold", 100, 20, 0.60)));
        runsPerAlgorithm.put("nsga2", List.of(createMetrics("nsga2", 70, 15, 0.75)));
        runsPerAlgorithm.put("hybrid", List.of(createMetrics("hybrid", 80, 18, 0.70)));

        ComparisonReport report = generator.generateReport("multi_algo", runsPerAlgorithm);

        assertNotNull(report);
        assertEquals(3, report.getAlgorithmResults().size());
        assertEquals("threshold", report.getBaselineAlgorithm(),
            "Should use 'threshold' as baseline when present");

        // Verify improvements calculated
        AlgorithmResult nsga2Result = findResult(report, "nsga2");
        assertNotNull(nsga2Result);
        assertTrue(nsga2Result.getPercentageImprovement().get("migrations") > 0,
            "NSGA2 should show improvement over baseline");
    }

    @Test
    public void testGenerateReportIdentifiesBaselineCorrectly() {
        // Test with threshold present
        Map<String, List<AlgorithmMetrics>> withThreshold = new HashMap<>();
        withThreshold.put("threshold", List.of(createMetrics("threshold", 100, 20, 0.60)));
        withThreshold.put("algorithm_a", List.of(createMetrics("algorithm_a", 90, 18, 0.65)));

        ComparisonReport report1 = generator.generateReport("scenario1", withThreshold);
        assertEquals("threshold", report1.getBaselineAlgorithm(),
            "Should prefer 'threshold' as baseline");

        // Test without threshold (alphabetical)
        Map<String, List<AlgorithmMetrics>> withoutThreshold = new HashMap<>();
        withoutThreshold.put("zebra", List.of(createMetrics("zebra", 100, 20, 0.60)));
        withoutThreshold.put("alpha", List.of(createMetrics("alpha", 90, 18, 0.65)));

        ComparisonReport report2 = generator.generateReport("scenario2", withoutThreshold);
        assertEquals("alpha", report2.getBaselineAlgorithm(),
            "Should use first alphabetically when 'threshold' not present");
    }

    @Test
    public void testGenerateReportUsesParetoCalculator() {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        // Create clearly dominated and non-dominated solutions
        runsPerAlgorithm.put("dominated", List.of(createMetrics("dominated", 150, 30, 0.50)));
        runsPerAlgorithm.put("optimal", List.of(createMetrics("optimal", 50, 10, 0.80)));

        ComparisonReport report = generator.generateReport("pareto_test", runsPerAlgorithm);

        assertTrue(report.getParetoFront().contains("optimal"),
            "Best algorithm should be on Pareto front");
        assertFalse(report.getParetoFront().contains("dominated"),
            "Clearly dominated algorithm should not be on Pareto front");

        AlgorithmResult optimalResult = findResult(report, "optimal");
        assertTrue(optimalResult.isParetoOptimal(),
            "Pareto-optimal flag should be set");
    }

    @Test
    public void testGenerateReportWithMultipleRuns() {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        // Multiple runs for threshold
        runsPerAlgorithm.put("threshold", List.of(
            createMetrics("threshold", 100, 20, 0.60),
            createMetrics("threshold", 102, 21, 0.61),
            createMetrics("threshold", 98, 19, 0.59)
        ));
        // Single run for nsga2
        runsPerAlgorithm.put("nsga2", List.of(createMetrics("nsga2", 70, 15, 0.75)));

        ComparisonReport report = generator.generateReport("multi_run", runsPerAlgorithm);

        assertNotNull(report);
        assertEquals(2, report.getAlgorithmResults().size());

        // Statistical tests should be empty because nsga2 has only 1 run
        // (needs both algorithms to have multiple runs for comparison)
        assertNotNull(report.getStatisticalTests());
    }

    @Test
    public void testFormatConsoleOutputProducesValidTable() {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        runsPerAlgorithm.put("threshold", List.of(createMetrics("threshold", 100, 20, 0.60)));
        runsPerAlgorithm.put("nsga2", List.of(createMetrics("nsga2", 70, 15, 0.75)));

        ComparisonReport report = generator.generateReport("format_test", runsPerAlgorithm);
        String output = generator.formatConsoleOutput(report);

        assertNotNull(output);
        assertTrue(output.contains("Algorithm Comparison Report"),
            "Should contain report title");
        assertTrue(output.contains("format_test"),
            "Should contain scenario ID");
        assertTrue(output.contains("threshold"),
            "Should contain threshold algorithm");
        assertTrue(output.contains("nsga2"),
            "Should contain nsga2 algorithm");
        assertTrue(output.contains("Migrations"),
            "Should contain Migrations metric");
        assertTrue(output.contains("Pareto Front"),
            "Should contain Pareto Front section");
        assertTrue(output.contains("WINNER SUMMARY"),
            "Should contain winner summary");
        assertTrue(output.contains("★"),
            "Should contain Pareto marker");
    }

    @Test
    public void testFormatConsoleOutputWithEmptyResults() {
        ComparisonReport emptyReport = new ComparisonReport(
            "empty", List.of(), "threshold", List.of(), Map.of()
        );

        String output = generator.formatConsoleOutput(emptyReport);

        assertNotNull(output);
        assertTrue(output.contains("No algorithm results available"),
            "Should handle empty results gracefully");
    }

    @Test
    public void testExportToCSVCreatesFileWithCorrectHeaders() throws IOException {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        runsPerAlgorithm.put("threshold", List.of(createMetrics("threshold", 100, 20, 0.60)));
        runsPerAlgorithm.put("nsga2", List.of(createMetrics("nsga2", 70, 15, 0.75)));

        ComparisonReport report = generator.generateReport("csv_test", runsPerAlgorithm);

        File csvFile = tempDir.resolve("comparison.csv").toFile();
        generator.exportToCSV(report, csvFile);

        assertTrue(csvFile.exists(), "CSV file should be created");

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertTrue(lines.size() > 0, "CSV should have content");

        String header = lines.get(0);
        assertTrue(header.contains("algorithm"), "Header should contain 'algorithm'");
        assertTrue(header.contains("migrations"), "Header should contain 'migrations'");
        assertTrue(header.contains("overloads"), "Header should contain 'overloads'");
        assertTrue(header.contains("utilization"), "Header should contain 'utilization'");
        assertTrue(header.contains("improvement_migrations_%"),
            "Header should contain improvement columns");
        assertTrue(header.contains("pareto_optimal"),
            "Header should contain pareto_optimal column");
    }

    @Test
    public void testExportToCSVWritesDataRows() throws IOException {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();
        runsPerAlgorithm.put("threshold", List.of(createMetrics("threshold", 100, 20, 0.60)));
        runsPerAlgorithm.put("nsga2", List.of(createMetrics("nsga2", 70, 15, 0.75)));

        ComparisonReport report = generator.generateReport("csv_data_test", runsPerAlgorithm);

        File csvFile = tempDir.resolve("comparison_data.csv").toFile();
        generator.exportToCSV(report, csvFile);

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertEquals(3, lines.size(), "Should have header + 2 data rows");

        // Check data rows contain algorithm names
        String allContent = String.join("\n", lines);
        assertTrue(allContent.contains("threshold"), "Should contain threshold data");
        assertTrue(allContent.contains("nsga2"), "Should contain nsga2 data");
    }

    /**
     * Helper method to create test AlgorithmMetrics.
     */
    private AlgorithmMetrics createMetrics(String name, int migrations, int overloads, double utilization) {
        return new AlgorithmMetrics.Builder()
            .algorithmName(name)
            .timestamp(100.0)
            .totalMigrations(migrations)
            .overloadEvents(overloads)
            .averageUtilization(utilization)
            .slaViolations(5)
            .decisionTimeMs(10)
            .build();
    }

    /**
     * Helper method to find an AlgorithmResult by name.
     */
    private AlgorithmResult findResult(ComparisonReport report, String algorithmName) {
        return report.getAlgorithmResults().stream()
            .filter(r -> r.getAlgorithmName().equals(algorithmName))
            .findFirst()
            .orElse(null);
    }
}
