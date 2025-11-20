package com.cloudsimulation.metrics;

import com.cloudsimulation.models.AlgorithmResult;
import com.cloudsimulation.models.ComparisonReport;
import com.cloudsimulation.models.StatisticalTest;
import com.cloudsimulation.utils.FileSecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates comparison reports for multi-algorithm benchmarking.
 * Provides report generation, console formatting, and CSV export capabilities.
 */
public class ComparisonReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ComparisonReportGenerator.class);

    private final ParetoCalculator paretoCalculator;
    private final TTestCalculator tTestCalculator;

    /**
     * Creates a new ComparisonReportGenerator.
     */
    public ComparisonReportGenerator() {
        this.paretoCalculator = new ParetoCalculator();
        this.tTestCalculator = new TTestCalculator();
    }

    /**
     * Generates comprehensive comparison report from multiple algorithm runs.
     *
     * @param scenarioId Scenario identifier
     * @param runsPerAlgorithm Map of algorithm name to list of metrics (one per run)
     * @return ComparisonReport with aggregated results and analysis
     */
    public ComparisonReport generateReport(String scenarioId,
                                          Map<String, List<AlgorithmMetrics>> runsPerAlgorithm) {
        logger.info("Generating comparison report for scenario: {}", scenarioId);

        // Aggregate metrics for each algorithm
        Map<String, AlgorithmMetrics> aggregatedMetrics = aggregateMetrics(runsPerAlgorithm);

        // Identify baseline (default: "threshold" or first alphabetically)
        String baselineAlgorithm = determineBaseline(runsPerAlgorithm.keySet());
        AlgorithmMetrics baselineMetrics = aggregatedMetrics.get(baselineAlgorithm);

        logger.info("Using baseline algorithm: {}", baselineAlgorithm);

        // Find Pareto-optimal solutions
        List<AlgorithmMetrics> allMetrics = new ArrayList<>(aggregatedMetrics.values());
        List<AlgorithmMetrics> paretoFrontMetrics = paretoCalculator.findNonDominatedSolutions(allMetrics);
        List<String> paretoFront = paretoFrontMetrics.stream()
            .map(AlgorithmMetrics::getAlgorithmName)
            .toList();

        logger.info("Pareto front contains {} algorithm(s): {}", paretoFront.size(), paretoFront);

        // Create algorithm results with improvement calculations
        List<AlgorithmResult> algorithmResults = new ArrayList<>();
        for (Map.Entry<String, AlgorithmMetrics> entry : aggregatedMetrics.entrySet()) {
            String algName = entry.getKey();
            AlgorithmMetrics metrics = entry.getValue();

            boolean isBaseline = algName.equals(baselineAlgorithm);
            boolean isParetoOptimal = paretoFront.contains(algName);

            AlgorithmResult result = new AlgorithmResult(algName, metrics, isBaseline, isParetoOptimal);
            result.calculateImprovementOverBaseline(baselineMetrics);
            algorithmResults.add(result);
        }

        // Perform statistical tests if multiple runs available
        Map<String, StatisticalTest> statisticalTests = new HashMap<>();
        if (hasMultipleRuns(runsPerAlgorithm)) {
            statisticalTests = performStatisticalTests(runsPerAlgorithm, baselineAlgorithm);
        }

        logger.info("Report generation complete: {} algorithms compared", algorithmResults.size());

        return new ComparisonReport(scenarioId, algorithmResults, baselineAlgorithm,
                                   paretoFront, statisticalTests);
    }

    /**
     * Aggregates metrics for each algorithm using MetricsAggregator.
     *
     * @param runsPerAlgorithm Map of algorithm runs
     * @return Map of algorithm name to aggregated metrics
     */
    private Map<String, AlgorithmMetrics> aggregateMetrics(
            Map<String, List<AlgorithmMetrics>> runsPerAlgorithm) {
        Map<String, AlgorithmMetrics> aggregatedMetrics = new HashMap<>();

        for (Map.Entry<String, List<AlgorithmMetrics>> entry : runsPerAlgorithm.entrySet()) {
            MetricsAggregator aggregator = new MetricsAggregator();
            for (AlgorithmMetrics metrics : entry.getValue()) {
                aggregator.addMetrics(metrics);
            }
            aggregatedMetrics.put(entry.getKey(), aggregator.getAggregated(entry.getKey()));
        }

        return aggregatedMetrics;
    }

    /**
     * Determines the baseline algorithm from available algorithms.
     * Priority: "threshold" > first alphabetically
     *
     * @param algorithmNames Set of algorithm names
     * @return Baseline algorithm name
     */
    private String determineBaseline(Set<String> algorithmNames) {
        if (algorithmNames.contains("threshold")) {
            return "threshold";
        }
        return algorithmNames.stream().sorted().findFirst().orElse("unknown");
    }

    /**
     * Checks if any algorithm has multiple runs.
     *
     * @param runsPerAlgorithm Map of algorithm runs
     * @return true if multiple runs exist, false otherwise
     */
    private boolean hasMultipleRuns(Map<String, List<AlgorithmMetrics>> runsPerAlgorithm) {
        return runsPerAlgorithm.values().stream().anyMatch(runs -> runs.size() > 1);
    }

    /**
     * Performs statistical significance tests comparing algorithms to baseline.
     *
     * @param runsPerAlgorithm Map of algorithm runs
     * @param baselineAlgorithm Baseline algorithm name
     * @return Map of metric names to statistical test results
     */
    private Map<String, StatisticalTest> performStatisticalTests(
            Map<String, List<AlgorithmMetrics>> runsPerAlgorithm, String baselineAlgorithm) {
        Map<String, StatisticalTest> tests = new HashMap<>();

        List<AlgorithmMetrics> baselineRuns = runsPerAlgorithm.get(baselineAlgorithm);
        if (baselineRuns == null || baselineRuns.size() < 2) {
            logger.warn("Insufficient baseline runs for statistical testing");
            return tests;
        }

        // Extract baseline metric samples
        List<Double> baselineMigrations = extractMetric(baselineRuns, AlgorithmMetrics::getTotalMigrations);
        List<Double> baselineOverloads = extractMetric(baselineRuns, AlgorithmMetrics::getOverloadEvents);
        List<Double> baselineUtilization = extractMetric(baselineRuns, AlgorithmMetrics::getAverageUtilization);

        // Compare each non-baseline algorithm
        for (Map.Entry<String, List<AlgorithmMetrics>> entry : runsPerAlgorithm.entrySet()) {
            String algName = entry.getKey();
            if (algName.equals(baselineAlgorithm) || entry.getValue().size() < 2) {
                continue;
            }

            List<AlgorithmMetrics> candidateRuns = entry.getValue();
            List<Double> candidateMigrations = extractMetric(candidateRuns, AlgorithmMetrics::getTotalMigrations);
            List<Double> candidateOverloads = extractMetric(candidateRuns, AlgorithmMetrics::getOverloadEvents);
            List<Double> candidateUtilization = extractMetric(candidateRuns, AlgorithmMetrics::getAverageUtilization);

            // Perform t-tests for each metric
            tests.put(algName + "_migrations",
                tTestCalculator.performTTest(baselineMigrations, candidateMigrations, "migrations"));
            tests.put(algName + "_overloads",
                tTestCalculator.performTTest(baselineOverloads, candidateOverloads, "overloads"));
            tests.put(algName + "_utilization",
                tTestCalculator.performTTest(baselineUtilization, candidateUtilization, "utilization"));
        }

        return tests;
    }

    /**
     * Extracts a specific metric from a list of AlgorithmMetrics.
     *
     * @param metrics List of metrics
     * @param extractor Function to extract metric value
     * @return List of extracted values
     */
    private List<Double> extractMetric(List<AlgorithmMetrics> metrics,
                                      java.util.function.ToDoubleFunction<AlgorithmMetrics> extractor) {
        return metrics.stream()
            .mapToDouble(extractor)
            .boxed()
            .toList();
    }

    /**
     * Formats comparison report as ASCII table for console output.
     *
     * @param report ComparisonReport to format
     * @return Formatted string with table and summary
     */
    public String formatConsoleOutput(ComparisonReport report) {
        StringBuilder sb = new StringBuilder();

        // Header
        sb.append("=".repeat(80)).append("\n");
        sb.append("Algorithm Comparison Report: ").append(report.getScenarioId()).append("\n");
        sb.append("=".repeat(80)).append("\n\n");

        // Build table
        List<AlgorithmResult> results = report.getAlgorithmResults();
        if (results.isEmpty()) {
            sb.append("No algorithm results available.\n");
            return sb.toString();
        }

        // Table header
        sb.append(String.format("%-20s", "Metric"));
        for (AlgorithmResult result : results) {
            String header = result.getAlgorithmName();
            if (result.isBaselineAlgorithm()) {
                header += " (baseline)";
            }
            sb.append(String.format(" | %-20s", header.length() > 20 ? header.substring(0, 17) + "..." : header));
        }
        sb.append("\n");
        sb.append("-".repeat(20));
        for (int i = 0; i < results.size(); i++) {
            sb.append("-|-").append("-".repeat(20));
        }
        sb.append("\n");

        // Metrics rows
        appendMetricRow(sb, "Migrations", results, r -> (double) r.getAggregatedMetrics().getTotalMigrations(), "migrations", true);
        appendMetricRow(sb, "Overload Events", results, r -> (double) r.getAggregatedMetrics().getOverloadEvents(), "overloads", true);
        appendMetricRow(sb, "Avg Utilization", results, r -> r.getAggregatedMetrics().getAverageUtilization(), "utilization", false);
        appendMetricRow(sb, "SLA Violations", results, r -> (double) r.getAggregatedMetrics().getSlaViolations(), "sla_violations", true);
        appendMetricRow(sb, "Decision Time (ms)", results, r -> (double) r.getAggregatedMetrics().getDecisionTimeMs(), "decision_time", true);

        // Legend
        sb.append("\n");
        sb.append("★ = Pareto-optimal solution\n");
        sb.append("- = Improvement (lower is better for migrations, overloads, SLA violations)\n");
        sb.append("+ = Improvement (higher is better for utilization)\n");

        // Winner summary
        sb.append("\nWINNER SUMMARY:\n");
        appendWinnerSummary(sb, results);

        // Pareto front
        sb.append("\nPareto Front: ").append(report.getParetoFront());
        sb.append(" (").append(report.getParetoFront().size()).append(" algorithm");
        if (report.getParetoFront().size() != 1) sb.append("s");
        sb.append(" on Pareto frontier)\n");

        sb.append("=".repeat(80)).append("\n");

        return sb.toString();
    }

    /**
     * Appends a metric row to the console output table.
     */
    private void appendMetricRow(StringBuilder sb, String metricName, List<AlgorithmResult> results,
                                 java.util.function.Function<AlgorithmResult, Double> valueExtractor,
                                 String improvementKey, boolean isMinimization) {
        sb.append(String.format("%-20s", metricName));

        for (AlgorithmResult result : results) {
            double value = valueExtractor.apply(result);
            String cellValue;

            if (result.isBaselineAlgorithm()) {
                cellValue = String.format("%.2f", value);
            } else {
                double improvement = result.getPercentageImprovement().get(improvementKey);
                String sign = improvement >= 0 ? "+" : "";
                cellValue = String.format("%.2f (%s%.1f%%)", value, sign, improvement);

                // Add Pareto marker if optimal
                if (result.isParetoOptimal()) {
                    cellValue += " ★";
                }
            }

            sb.append(String.format(" | %-20s", cellValue.length() > 20 ? cellValue.substring(0, 17) + "..." : cellValue));
        }
        sb.append("\n");
    }

    /**
     * Appends winner summary section to console output.
     */
    private void appendWinnerSummary(StringBuilder sb, List<AlgorithmResult> results) {
        // Find best for each metric
        AlgorithmResult bestMigrations = findBest(results, r -> (double) r.getAggregatedMetrics().getTotalMigrations(), true);
        AlgorithmResult bestOverloads = findBest(results, r -> (double) r.getAggregatedMetrics().getOverloadEvents(), true);
        AlgorithmResult bestUtilization = findBest(results, r -> r.getAggregatedMetrics().getAverageUtilization(), false);
        AlgorithmResult bestSLA = findBest(results, r -> (double) r.getAggregatedMetrics().getSlaViolations(), true);
        AlgorithmResult bestDecisionTime = findBest(results, r -> (double) r.getAggregatedMetrics().getDecisionTimeMs(), true);

        sb.append(String.format("  Migrations: %s (%d migrations", bestMigrations.getAlgorithmName(),
            bestMigrations.getAggregatedMetrics().getTotalMigrations()));
        if (!bestMigrations.isBaselineAlgorithm()) {
            sb.append(String.format(", %.1f%% improvement", bestMigrations.getPercentageImprovement().get("migrations")));
        }
        sb.append(")\n");

        sb.append(String.format("  Overloads: %s (%d events", bestOverloads.getAlgorithmName(),
            bestOverloads.getAggregatedMetrics().getOverloadEvents()));
        if (!bestOverloads.isBaselineAlgorithm()) {
            sb.append(String.format(", %.1f%% improvement", bestOverloads.getPercentageImprovement().get("overloads")));
        }
        sb.append(")\n");

        sb.append(String.format("  Utilization: %s (%.4f", bestUtilization.getAlgorithmName(),
            bestUtilization.getAggregatedMetrics().getAverageUtilization()));
        if (!bestUtilization.isBaselineAlgorithm()) {
            sb.append(String.format(", %.1f%% improvement", bestUtilization.getPercentageImprovement().get("utilization")));
        }
        sb.append(")\n");

        sb.append(String.format("  SLA Violations: %s (%d violations", bestSLA.getAlgorithmName(),
            bestSLA.getAggregatedMetrics().getSlaViolations()));
        if (!bestSLA.isBaselineAlgorithm()) {
            sb.append(String.format(", %.1f%% improvement", bestSLA.getPercentageImprovement().get("sla_violations")));
        }
        if (bestSLA.isParetoOptimal()) {
            sb.append(" ★");
        }
        sb.append(")\n");

        sb.append(String.format("  Decision Speed: %s (%dms", bestDecisionTime.getAlgorithmName(),
            bestDecisionTime.getAggregatedMetrics().getDecisionTimeMs()));
        if (!bestDecisionTime.isBaselineAlgorithm()) {
            sb.append(", baseline");
        }
        sb.append(")\n");
    }

    /**
     * Finds the best algorithm for a given metric.
     */
    private AlgorithmResult findBest(List<AlgorithmResult> results,
                                    java.util.function.Function<AlgorithmResult, Double> valueExtractor,
                                    boolean minimize) {
        return results.stream()
            .min((r1, r2) -> {
                double v1 = valueExtractor.apply(r1);
                double v2 = valueExtractor.apply(r2);
                return minimize ? Double.compare(v1, v2) : Double.compare(v2, v1);
            })
            .orElse(results.get(0));
    }

    /**
     * Exports comparison report to CSV file.
     *
     * @param report ComparisonReport to export
     * @param outputFile Target CSV file
     * @throws IOException if export fails
     */
    public void exportToCSV(ComparisonReport report, File outputFile) throws IOException {
        String sanitizedPath = FileSecurityValidator.sanitizePath(outputFile.getPath());

        logger.info("Exporting comparison report to CSV: {}", sanitizedPath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(sanitizedPath))) {
            // Write headers
            writer.write("algorithm,migrations,overloads,utilization,sla_violations,decision_time_ms,");
            writer.write("improvement_migrations_%,improvement_overloads_%,improvement_utilization_%,");
            writer.write("improvement_sla_violations_%,improvement_decision_time_%,pareto_optimal");
            writer.newLine();

            // Write data rows
            for (AlgorithmResult result : report.getAlgorithmResults()) {
                AlgorithmMetrics metrics = result.getAggregatedMetrics();
                Map<String, Double> improvements = result.getPercentageImprovement();

                writer.write(String.format("%s,%d,%d,%.4f,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%s",
                    result.getAlgorithmName(),
                    metrics.getTotalMigrations(),
                    metrics.getOverloadEvents(),
                    metrics.getAverageUtilization(),
                    metrics.getSlaViolations(),
                    metrics.getDecisionTimeMs(),
                    improvements.get("migrations"),
                    improvements.get("overloads"),
                    improvements.get("utilization"),
                    improvements.get("sla_violations"),
                    improvements.get("decision_time"),
                    result.isParetoOptimal()));
                writer.newLine();
            }

            writer.flush();
        }

        logger.info("CSV export complete: {}", sanitizedPath);
    }
}
