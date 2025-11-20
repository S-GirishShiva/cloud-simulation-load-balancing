package com.cloudsimulation.io;

import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.utils.FileSecurityValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Exports simulation metrics to CSV format for analysis in Excel, Python, or R.
 * Supports incremental writing during simulation for real-time data export.
 */
public class CSVExporter {
    private static final Logger logger = LoggerFactory.getLogger(CSVExporter.class);
    private static final String METRICS_FILE_NAME = "metrics.csv";
    private static final String RESULTS_BASE_DIR = "results";
    private static final int MAX_ROWS = 40_000; // 1MB limit at ~26 bytes/row
    private static final int WARN_THRESHOLD_ROWS = 32_000; // 80% of MAX_ROWS
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final AtomicInteger runCounter = new AtomicInteger(0);

    private final String outputDirectory;
    private BufferedWriter writer;
    private int rowCount;
    private boolean closed;
    private File currentRunDirectory;

    /**
     * Creates a new CSVExporter with the default output directory.
     */
    public CSVExporter() {
        this(RESULTS_BASE_DIR);
    }

    /**
     * Creates a new CSVExporter with a custom output directory.
     *
     * @param outputDirectory Base directory for results output
     */
    public CSVExporter(String outputDirectory) {
        this.outputDirectory = outputDirectory;
        this.rowCount = 0;
        this.closed = false;
    }

    /**
     * Initializes a new simulation run, creating the directory structure and CSV file.
     * Directory structure: {outputDirectory}/{scenarioId}/run_{timestamp}_{id}/
     *
     * @param scenarioId Unique identifier for the simulation scenario
     * @return Unique run identifier for this simulation
     * @throws IOException if directory creation or file initialization fails
     */
    public String initializeRun(String scenarioId) throws IOException {
        // Sanitize scenario ID to prevent directory traversal attacks
        String sanitizedScenarioId = FileSecurityValidator.sanitizePath(scenarioId);

        // Create scenario directory
        File scenarioDir = new File(outputDirectory, sanitizedScenarioId);
        if (!scenarioDir.exists() && !scenarioDir.mkdirs()) {
            throw new IOException("Failed to create scenario directory: " + scenarioDir.getAbsolutePath());
        }

        // Generate unique run ID
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        int runNumber = runCounter.incrementAndGet();
        String runId = String.format("run_%s_%03d", timestamp, runNumber);

        // Create run directory
        currentRunDirectory = new File(scenarioDir, runId);
        if (!currentRunDirectory.mkdirs()) {
            throw new IOException("Failed to create run directory: " + currentRunDirectory.getAbsolutePath());
        }

        // Create CSV file
        File csvFile = new File(currentRunDirectory, METRICS_FILE_NAME);
        writer = new BufferedWriter(new FileWriter(csvFile));

        // Write CSV headers
        writeHeaders();

        logger.info("Initialized CSV export for run: {} at {}", runId, csvFile.getAbsolutePath());
        return runId;
    }

    /**
     * Writes CSV column headers.
     *
     * @throws IOException if writing fails
     */
    private void writeHeaders() throws IOException {
        writer.write("timestamp,avg_cpu_util,avg_mem_util,overloaded_vm_count,total_migrations");
        writer.newLine();
        writer.flush();
    }

    /**
     * Appends a metrics snapshot to the CSV file.
     * Writes data incrementally and flushes immediately for real-time export.
     *
     * @param snapshot The metrics snapshot to export
     * @throws IOException if writing fails
     * @throws IllegalStateException if called after close() or row limit exceeded
     */
    public synchronized void appendMetrics(MetricsSnapshot snapshot) throws IOException {
        if (closed) {
            throw new IllegalStateException("Cannot append metrics after CSVExporter has been closed");
        }

        if (writer == null) {
            throw new IllegalStateException("CSVExporter not initialized. Call initializeRun() first.");
        }

        // Check row limit to prevent unbounded file growth
        if (rowCount >= MAX_ROWS) {
            throw new IllegalStateException("CSV size limit exceeded: " + MAX_ROWS + " rows");
        }

        // Log warning at 80% capacity
        if (rowCount == WARN_THRESHOLD_ROWS) {
            logger.warn("CSV file approaching size limit: {} of {} rows", rowCount, MAX_ROWS);
        }

        // Format and write row
        String row = formatMetricsRow(snapshot);
        writer.write(row);
        writer.newLine();
        writer.flush();

        rowCount++;
    }

    /**
     * Formats a MetricsSnapshot as a CSV row.
     * Format: timestamp (1 decimal), utilization (4 decimals), counts (integers)
     *
     * @param snapshot The metrics snapshot to format
     * @return CSV-formatted row string
     */
    private String formatMetricsRow(MetricsSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("MetricsSnapshot cannot be null");
        }

        return String.format("%.1f,%.4f,%.4f,%d,%d",
            snapshot.getTimestamp(),
            snapshot.getAvgCpuUtilization(),
            snapshot.getAvgMemoryUtilization(),
            snapshot.getOverloadedVmCount(),
            snapshot.getTotalMigrations());
    }

    /**
     * Closes the CSV file and releases resources.
     * Safe to call multiple times. After closing, appendMetrics() will throw exception.
     */
    public void close() {
        if (closed) {
            return; // Already closed
        }

        if (writer != null) {
            try {
                writer.close();
                logger.info("Closed CSV export. Total rows written: {}", rowCount);
            } catch (IOException e) {
                logger.error("Error closing CSV writer", e);
            }
        }

        closed = true;
    }

    /**
     * Gets the current run directory.
     *
     * @return The directory for the current run, or null if not initialized
     */
    public File getCurrentRunDirectory() {
        return currentRunDirectory;
    }

    /**
     * Gets the current row count.
     *
     * @return Number of data rows written (excludes header)
     */
    public int getRowCount() {
        return rowCount;
    }
}
