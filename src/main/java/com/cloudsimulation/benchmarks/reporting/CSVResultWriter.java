package com.cloudsimulation.benchmarks.reporting;

import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Writes benchmark results to CSV files for trending analysis.
 * Supports append mode for accumulating historical data.
 *
 * CSV Schema: timestamp,benchmark,metric,value,unit,profile,metadata
 */
public class CSVResultWriter {
    private static final Logger logger = LoggerFactory.getLogger(CSVResultWriter.class);

    private static final String CSV_HEADER = "timestamp,benchmark,metric,value,unit,profile,metadata";
    private static final String DEFAULT_RESULTS_DIR = "target/benchmarks";
    private static final String RESULTS_FILE = "benchmark-results.csv";

    private final String resultsDirectory;
    private final Path resultsFilePath;

    public CSVResultWriter() {
        this(DEFAULT_RESULTS_DIR);
    }

    public CSVResultWriter(String resultsDirectory) {
        this.resultsDirectory = resultsDirectory;
        this.resultsFilePath = Paths.get(resultsDirectory, RESULTS_FILE);
    }

    /**
     * Write benchmark results to CSV file (append mode)
     *
     * @param results List of benchmark results to write
     * @throws IOException if writing fails
     */
    public void writeResults(List<BenchmarkResult> results) throws IOException {
        ensureDirectoryExists();
        ensureHeaderExists();

        logger.info("Writing {} benchmark results to {}", results.size(), resultsFilePath);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(resultsFilePath.toFile(), true))) {
            for (BenchmarkResult result : results) {
                writeResult(writer, result);
            }
        }

        logger.info("Successfully wrote results to CSV");
    }

    /**
     * Write a single benchmark result to the writer
     */
    private void writeResult(BufferedWriter writer, BenchmarkResult result) throws IOException {
        String timestamp = result.getTimestampISO();
        String benchmarkName = result.getBenchmarkName();
        String profile = result.getProfile();

        // Convert metadata map to semicolon-separated string
        String metadata = formatMetadata(result.getMetadata());

        // Write one row per metric
        for (Map.Entry<String, Double> metric : result.getMetrics().entrySet()) {
            String metricName = metric.getKey();
            double value = metric.getValue();
            String unit = inferUnit(metricName);

            String row = String.format("%s,%s,%s,%.4f,%s,%s,\"%s\"%n",
                    timestamp, benchmarkName, metricName, value, unit, profile, metadata);
            writer.write(row);
        }

        // If benchmark failed, write a special row
        if (!result.isPassed()) {
            String row = String.format("%s,%s,status,0.0,boolean,%s,\"FAILED: %s\"%n",
                    timestamp, benchmarkName, profile, result.getFailureReason());
            writer.write(row);
        }
    }

    /**
     * Infer the unit for a metric based on its name
     */
    private String inferUnit(String metricName) {
        if (metricName.contains("_ms") || metricName.contains("time_ms")) {
            return "ms";
        } else if (metricName.contains("_mb") || metricName.contains("memory_mb")) {
            return "MB";
        } else if (metricName.contains("per_second") || metricName.contains("per_sec")) {
            return "ops/s";
        } else if (metricName.contains("percent")) {
            return "%";
        } else {
            return "count";
        }
    }

    /**
     * Format metadata map as semicolon-separated key=value pairs
     */
    private String formatMetadata(Map<String, String> metadata) {
        if (metadata.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : metadata.entrySet()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

    /**
     * Ensure the results directory exists
     */
    private void ensureDirectoryExists() throws IOException {
        File dir = new File(resultsDirectory);
        if (!dir.exists()) {
            logger.info("Creating results directory: {}", resultsDirectory);
            Files.createDirectories(dir.toPath());
        }
    }

    /**
     * Ensure CSV file has header row
     */
    private void ensureHeaderExists() throws IOException {
        File file = resultsFilePath.toFile();
        if (!file.exists() || file.length() == 0) {
            logger.info("Creating new CSV file with header: {}", resultsFilePath);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write(CSV_HEADER);
                writer.newLine();
            }
        }
    }

    /**
     * Get the path to the results file
     */
    public Path getResultsFilePath() {
        return resultsFilePath;
    }

    /**
     * Clear all existing results (for testing)
     */
    public void clearResults() throws IOException {
        File file = resultsFilePath.toFile();
        if (file.exists()) {
            logger.warn("Deleting existing results file: {}", resultsFilePath);
            file.delete();
        }
    }

    /**
     * Archive old results by renaming with timestamp
     */
    public void archiveResults() throws IOException {
        File file = resultsFilePath.toFile();
        if (file.exists()) {
            String timestamp = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String archiveName = "benchmark-results-" + timestamp + ".csv";
            Path archivePath = Paths.get(resultsDirectory, archiveName);

            logger.info("Archiving results to: {}", archivePath);
            Files.move(resultsFilePath, archivePath);
        }
    }
}
