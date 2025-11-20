package com.cloudsimulation.benchmarks.reporting;

import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Manages baseline performance data and detects regressions.
 * Compares current benchmark results against baseline and flags >10% degradation.
 */
public class BaselineManager {
    private static final Logger logger = LoggerFactory.getLogger(BaselineManager.class);

    private static final double REGRESSION_THRESHOLD = 0.10; // 10%
    private static final String DEFAULT_BASELINE_DIR = "target/benchmarks";
    private static final String BASELINE_FILE_PATTERN = "baseline-%s.csv";

    private final String baselineDirectory;
    private final Map<String, Map<String, Double>> baselineData;  // benchmark -> metric -> value

    public BaselineManager() {
        this(DEFAULT_BASELINE_DIR);
    }

    public BaselineManager(String baselineDirectory) {
        this.baselineDirectory = baselineDirectory;
        this.baselineData = new HashMap<>();
    }

    /**
     * Load baseline data for a specific profile
     */
    public void loadBaseline(String profile) throws IOException {
        Path baselinePath = getBaselinePath(profile);

        if (!Files.exists(baselinePath)) {
            logger.warn("No baseline file found for profile '{}' at: {}", profile, baselinePath);
            logger.warn("First run will establish baseline");
            return;
        }

        logger.info("Loading baseline from: {}", baselinePath);
        baselineData.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(baselinePath.toFile()))) {
            String line;
            reader.readLine(); // Skip header

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 4) {
                    String benchmark = parts[1];
                    String metric = parts[2];
                    double value = Double.parseDouble(parts[3]);

                    baselineData.computeIfAbsent(benchmark, k -> new HashMap<>())
                            .put(metric, value);
                }
            }
        }

        logger.info("Loaded baseline data for {} benchmarks", baselineData.size());
    }

    /**
     * Save current results as new baseline
     */
    public void saveBaseline(List<BenchmarkResult> results, String profile) throws IOException {
        Path baselinePath = getBaselinePath(profile);
        Files.createDirectories(baselinePath.getParent());

        logger.info("Saving baseline to: {}", baselinePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(baselinePath.toFile()))) {
            // Write header
            writer.write("timestamp,benchmark,metric,value");
            writer.newLine();

            // Write results
            for (BenchmarkResult result : results) {
                String timestamp = result.getTimestampISO();
                String benchmark = result.getBenchmarkName();

                for (Map.Entry<String, Double> metric : result.getMetrics().entrySet()) {
                    String metricName = metric.getKey();
                    double value = metric.getValue();

                    writer.write(String.format("%s,%s,%s,%.4f%n",
                            timestamp, benchmark, metricName, value));
                }
            }
        }

        logger.info("Baseline saved successfully");
    }

    /**
     * Compare current results against baseline and detect regressions
     *
     * @return List of regression descriptions, empty if no regressions
     */
    public List<String> detectRegressions(List<BenchmarkResult> currentResults) {
        List<String> regressions = new ArrayList<>();

        if (baselineData.isEmpty()) {
            logger.info("No baseline data loaded - skipping regression detection");
            return regressions;
        }

        logger.info("Checking for performance regressions (threshold: {}%)", REGRESSION_THRESHOLD * 100);

        for (BenchmarkResult result : currentResults) {
            String benchmark = result.getBenchmarkName();
            Map<String, Double> baselineMetrics = baselineData.get(benchmark);

            if (baselineMetrics == null) {
                logger.warn("No baseline data for benchmark: {}", benchmark);
                continue;
            }

            for (Map.Entry<String, Double> metric : result.getMetrics().entrySet()) {
                String metricName = metric.getKey();
                double currentValue = metric.getValue();

                Double baselineValue = baselineMetrics.get(metricName);
                if (baselineValue == null) {
                    continue;
                }

                // Check for regression
                boolean isRegression = hasRegression(metricName, baselineValue, currentValue);
                if (isRegression) {
                    double percentChange = ((currentValue - baselineValue) / baselineValue) * 100;
                    String regression = String.format(
                            "REGRESSION in %s.%s: %.2f -> %.2f (%.1f%% %s)",
                            benchmark, metricName, baselineValue, currentValue,
                            Math.abs(percentChange),
                            percentChange > 0 ? "increase" : "decrease");
                    regressions.add(regression);
                    logger.warn(regression);
                }
            }
        }

        if (regressions.isEmpty()) {
            logger.info("No regressions detected - all metrics within threshold");
        } else {
            logger.warn("Detected {} performance regressions", regressions.size());
        }

        return regressions;
    }

    /**
     * Determine if there is a regression for a metric
     * For time/memory metrics: increase is bad (regression)
     * For throughput metrics: decrease is bad (regression)
     */
    private boolean hasRegression(String metricName, double baseline, double current) {
        double percentChange = (current - baseline) / baseline;

        // Metrics where lower is better (time, memory)
        if (isLowerBetter(metricName)) {
            return percentChange > REGRESSION_THRESHOLD;  // Increase is bad
        }
        // Metrics where higher is better (throughput)
        else {
            return percentChange < -REGRESSION_THRESHOLD;  // Decrease is bad
        }
    }

    /**
     * Determine if lower values are better for this metric
     */
    private boolean isLowerBetter(String metricName) {
        String lower = metricName.toLowerCase();
        return lower.contains("time") ||
               lower.contains("memory") ||
               lower.contains("pause") ||
               lower.contains("latency") ||
               lower.contains("_ms") ||
               lower.contains("_mb");
    }

    /**
     * Get baseline file path for a profile
     */
    private Path getBaselinePath(String profile) {
        String filename = String.format(BASELINE_FILE_PATTERN, profile);
        return Paths.get(baselineDirectory, filename);
    }

    /**
     * Check if baseline exists for a profile
     */
    public boolean hasBaseline(String profile) {
        return Files.exists(getBaselinePath(profile));
    }
}
