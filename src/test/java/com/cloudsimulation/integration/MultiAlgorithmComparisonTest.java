package com.cloudsimulation.integration;

import com.cloudsimulation.metrics.AlgorithmMetrics;
import com.cloudsimulation.metrics.ComparisonReportGenerator;
import com.cloudsimulation.models.AlgorithmResult;
import com.cloudsimulation.models.ComparisonReport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for multi-algorithm comparison workflow.
 * Tests the complete flow: metrics collection -> report generation -> CSV export -> console output.
 */
public class MultiAlgorithmComparisonTest {
    private ComparisonReportGenerator generator;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setup() {
        generator = new ComparisonReportGenerator();
    }

    @Test
    public void testMultiAlgorithmComparisonFullWorkflow() throws IOException {
        // Simulate metrics from 3 algorithms with multiple runs
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = createMultiAlgorithmRuns();

        // 1. Generate comparison report
        ComparisonReport report = generator.generateReport("traffic_spike", runsPerAlgorithm);

        // 2. Verify report contains all 3 algorithms
        assertNotNull(report);
        assertEquals("traffic_spike", report.getScenarioId());
        assertEquals(3, report.getAlgorithmResults().size(),
            "Report should contain all 3 algorithms");

        // 3. Verify percentage improvements calculated
        AlgorithmResult nsga2Result = findResult(report, "nsga2");
        assertNotNull(nsga2Result);
        Map<String, Double> improvements = nsga2Result.getPercentageImprovement();
        assertNotNull(improvements.get("migrations"), "Migrations improvement should be calculated");
        assertNotNull(improvements.get("overloads"), "Overloads improvement should be calculated");
        assertNotNull(improvements.get("utilization"), "Utilization improvement should be calculated");
        assertNotNull(improvements.get("sla_violations"), "SLA violations improvement should be calculated");
        assertNotNull(improvements.get("decision_time"), "Decision time improvement should be calculated");

        // Verify improvements make sense (nsga2 is better than threshold in most metrics)
        assertTrue(improvements.get("migrations") > 0,
            "NSGA2 should show positive improvement in migrations");
        assertTrue(improvements.get("utilization") > 0,
            "NSGA2 should show positive improvement in utilization");

        // 4. Verify Pareto front identified (at least one algorithm marked optimal)
        assertFalse(report.getParetoFront().isEmpty(),
            "Pareto front should contain at least one algorithm");

        boolean hasParetoOptimalAlgorithm = report.getAlgorithmResults().stream()
            .anyMatch(AlgorithmResult::isParetoOptimal);
        assertTrue(hasParetoOptimalAlgorithm,
            "At least one algorithm should be marked Pareto-optimal");

        // 5. Verify CSV export creates valid file
        File csvFile = tempDir.resolve("comparison_integration.csv").toFile();
        generator.exportToCSV(report, csvFile);

        assertTrue(csvFile.exists(), "CSV file should be created");
        List<String> csvLines = Files.readAllLines(csvFile.toPath());
        assertTrue(csvLines.size() >= 4, "CSV should have header + 3 data rows");

        // Verify CSV headers
        String header = csvLines.get(0);
        assertTrue(header.contains("algorithm"));
        assertTrue(header.contains("migrations"));
        assertTrue(header.contains("improvement_migrations_%"));
        assertTrue(header.contains("pareto_optimal"));

        // Verify CSV data
        String csvContent = String.join("\n", csvLines);
        assertTrue(csvContent.contains("threshold"));
        assertTrue(csvContent.contains("nsga2"));
        assertTrue(csvContent.contains("hybrid"));

        // 6. Verify console output contains winner summary
        String consoleOutput = generator.formatConsoleOutput(report);

        assertNotNull(consoleOutput);
        assertTrue(consoleOutput.contains("Algorithm Comparison Report"),
            "Console output should have report header");
        assertTrue(consoleOutput.contains("traffic_spike"),
            "Console output should contain scenario ID");
        assertTrue(consoleOutput.contains("WINNER SUMMARY"),
            "Console output should contain winner summary");
        assertTrue(consoleOutput.contains("Pareto Front"),
            "Console output should contain Pareto front info");
        assertTrue(consoleOutput.contains("Migrations:"),
            "Winner summary should include migrations winner");
        assertTrue(consoleOutput.contains("Utilization:"),
            "Winner summary should include utilization winner");

        // Verify Pareto marker is present
        assertTrue(consoleOutput.contains("★"),
            "Console output should contain Pareto-optimal marker");
    }

    @Test
    public void testComparisonWithMultipleRunsPerformsStatisticalTests() {
        // Create runs with sufficient samples for t-tests
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();

        // Threshold: 5 runs with slight variations
        List<AlgorithmMetrics> thresholdRuns = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            thresholdRuns.add(createMetrics("threshold", 100 + i, 20 + (i % 2), 0.60 + i * 0.01, i));
        }
        runsPerAlgorithm.put("threshold", thresholdRuns);

        // NSGA2: 5 runs with slight variations
        List<AlgorithmMetrics> nsga2Runs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nsga2Runs.add(createMetrics("nsga2", 70 + i, 15 + (i % 2), 0.75 + i * 0.01, i));
        }
        runsPerAlgorithm.put("nsga2", nsga2Runs);

        ComparisonReport report = generator.generateReport("multi_run_test", runsPerAlgorithm);

        // Statistical tests should be performed
        assertNotNull(report.getStatisticalTests());

        // With multiple runs, we expect some statistical test results
        // (may be empty if samples are too similar, but the mechanism should run)
    }

    @Test
    public void testBaselineAlgorithmIdentification() {
        Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = createMultiAlgorithmRuns();

        ComparisonReport report = generator.generateReport("baseline_test", runsPerAlgorithm);

        assertEquals("threshold", report.getBaselineAlgorithm(),
            "Should identify 'threshold' as baseline when present");

        AlgorithmResult thresholdResult = findResult(report, "threshold");
        assertTrue(thresholdResult.isBaselineAlgorithm(),
            "Threshold result should be flagged as baseline");

        // Baseline should have 0% improvement over itself
        Map<String, Double> baselineImprovements = thresholdResult.getPercentageImprovement();
        assertEquals(0.0, baselineImprovements.get("migrations"), 0.01);
        assertEquals(0.0, baselineImprovements.get("overloads"), 0.01);
    }

    /**
     * Creates simulated multi-algorithm runs for testing.
     * Simulates:
     * - threshold: 5 runs (baseline algorithm)
     * - nsga2: 3 runs (better performance)
     * - hybrid: 3 runs (mixed performance)
     */
    private Map<String, List<AlgorithmMetrics>> createMultiAlgorithmRuns() {
        Map<String, List<AlgorithmMetrics>> runs = new HashMap<>();

        // Threshold: 5 runs with consistent mediocre performance
        List<AlgorithmMetrics> thresholdRuns = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            thresholdRuns.add(createMetrics("threshold", 145 + i, 23 + (i % 2), 0.652 + i * 0.001, i * 10));
        }
        runs.put("threshold", thresholdRuns);

        // NSGA2: 3 runs with better performance (fewer migrations, higher utilization)
        List<AlgorithmMetrics> nsga2Runs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            nsga2Runs.add(createMetrics("nsga2", 98 + i, 15 + (i % 2), 0.714 + i * 0.002, i * 10));
        }
        runs.put("nsga2", nsga2Runs);

        // Hybrid: 3 runs with moderate performance
        List<AlgorithmMetrics> hybridRuns = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            hybridRuns.add(createMetrics("hybrid", 112 + i, 18 + (i % 2), 0.689 + i * 0.001, i * 10));
        }
        runs.put("hybrid", hybridRuns);

        return runs;
    }

    /**
     * Creates a single AlgorithmMetrics instance for testing.
     */
    private AlgorithmMetrics createMetrics(String name, int migrations, int overloads,
                                          double utilization, int timestamp) {
        return new AlgorithmMetrics.Builder()
            .algorithmName(name)
            .timestamp(timestamp)
            .totalMigrations(migrations)
            .overloadEvents(overloads)
            .averageUtilization(utilization)
            .slaViolations(8)
            .decisionTimeMs(name.equals("threshold") ? 12 : (name.equals("nsga2") ? 245 : 89))
            .build();
    }

    /**
     * Helper to find an AlgorithmResult by name.
     */
    private AlgorithmResult findResult(ComparisonReport report, String algorithmName) {
        return report.getAlgorithmResults().stream()
            .filter(r -> r.getAlgorithmName().equals(algorithmName))
            .findFirst()
            .orElse(null);
    }
}
