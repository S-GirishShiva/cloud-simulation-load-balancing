package com.cloudsimulation.io;

import com.cloudsimulation.metrics.MetricsSnapshot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for CSVExporter class.
 * Tests CSV export functionality, file formatting, size validation, and resource management.
 */
class CSVExporterTest {

    private CSVExporter exporter;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        // Create temporary directory for test outputs
        tempDir = Files.createTempDirectory("csvexporter_test_");
        exporter = new CSVExporter(tempDir.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        // Close exporter if still open
        if (exporter != null) {
            exporter.close();
        }

        // Clean up temporary test files
        if (tempDir != null && Files.exists(tempDir)) {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    void testInitializeRunCreatesDirectory() throws IOException {
        // Arrange
        String scenarioId = "test_scenario";

        // Act
        String runId = exporter.initializeRun(scenarioId);

        // Assert
        assertNotNull(runId, "Run ID should not be null");
        assertTrue(runId.startsWith("run_"), "Run ID should start with 'run_'");

        File scenarioDir = new File(tempDir.toFile(), scenarioId);
        assertTrue(scenarioDir.exists(), "Scenario directory should exist");
        assertTrue(scenarioDir.isDirectory(), "Scenario path should be a directory");

        File runDir = exporter.getCurrentRunDirectory();
        assertNotNull(runDir, "Run directory should not be null");
        assertTrue(runDir.exists(), "Run directory should exist");
        assertTrue(runDir.getName().equals(runId), "Run directory name should match run ID");

        File csvFile = new File(runDir, "metrics.csv");
        assertTrue(csvFile.exists(), "CSV file should be created");
        assertTrue(csvFile.length() > 0, "CSV file should contain headers");
    }

    @Test
    void testCSVHeadersMatchSpecification() throws IOException {
        // Arrange
        exporter.initializeRun("test_headers");

        // Act
        File csvFile = new File(exporter.getCurrentRunDirectory(), "metrics.csv");

        // Assert
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            assertEquals("timestamp,avg_cpu_util,avg_mem_util,overloaded_vm_count,total_migrations",
                headerLine, "CSV headers must match specification exactly");
        }
    }

    @Test
    void testAppendMetricsWritesCorrectFormat() throws IOException {
        // Arrange
        exporter.initializeRun("test_format");
        MetricsSnapshot snapshot = new MetricsSnapshot(
            10.0,      // timestamp
            0.4523,    // avgCpuUtilization
            0.3812,    // avgMemoryUtilization
            2,         // overloadedVmCount
            1,         // underloadedVmCount
            5,         // totalMigrations
            100,       // activeVmCount
            250.5      // powerConsumption
        );

        // Act
        exporter.appendMetrics(snapshot);
        exporter.close();

        // Assert
        File csvFile = new File(exporter.getCurrentRunDirectory(), "metrics.csv");
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            reader.readLine(); // Skip header
            String dataLine = reader.readLine();

            // Verify format: "%.1f,%.4f,%.4f,%d,%d"
            assertEquals("10.0,0.4523,0.3812,2,5", dataLine,
                "CSV row format must match specification");
        }
    }

    @Test
    void testIncrementalWriting() throws IOException {
        // Arrange
        exporter.initializeRun("test_incremental");
        MetricsSnapshot snapshot1 = createSnapshot(0.0, 0.1, 0.2, 1, 0);
        MetricsSnapshot snapshot2 = createSnapshot(10.0, 0.3, 0.4, 2, 1);
        MetricsSnapshot snapshot3 = createSnapshot(20.0, 0.5, 0.6, 3, 2);

        // Act
        exporter.appendMetrics(snapshot1);
        exporter.appendMetrics(snapshot2);
        exporter.appendMetrics(snapshot3);
        exporter.close();

        // Assert
        File csvFile = new File(exporter.getCurrentRunDirectory(), "metrics.csv");
        List<String> lines = Files.readAllLines(csvFile.toPath());

        assertEquals(4, lines.size(), "CSV should have 1 header + 3 data rows");
        assertEquals("timestamp,avg_cpu_util,avg_mem_util,overloaded_vm_count,total_migrations",
            lines.get(0), "First line should be headers");
        assertEquals("0.0,0.1000,0.2000,1,0", lines.get(1), "First data row incorrect");
        assertEquals("10.0,0.3000,0.4000,2,1", lines.get(2), "Second data row incorrect");
        assertEquals("20.0,0.5000,0.6000,3,2", lines.get(3), "Third data row incorrect");
    }

    @Test
    void testFileSizeValidation() throws IOException {
        // Arrange
        exporter.initializeRun("test_size_limit");

        // Act & Assert
        // Write up to MAX_ROWS (40,000)
        for (int i = 0; i < 40_000; i++) {
            MetricsSnapshot snapshot = createSnapshot(i, 0.5, 0.5, 0, 0);
            exporter.appendMetrics(snapshot);
        }

        assertEquals(40_000, exporter.getRowCount(), "Should write exactly 40,000 rows");

        // Attempting to write 40,001st row should throw exception
        MetricsSnapshot extraSnapshot = createSnapshot(40_000, 0.5, 0.5, 0, 0);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> exporter.appendMetrics(extraSnapshot),
            "Should throw exception when exceeding MAX_ROWS");

        assertTrue(exception.getMessage().contains("CSV size limit exceeded"),
            "Exception message should indicate size limit exceeded");
    }

    @Test
    void testExcelCompatibility() throws IOException {
        // Arrange
        exporter.initializeRun("test_excel");
        MetricsSnapshot snapshot = createSnapshot(15.0, 0.7825, 0.6543, 4, 7);

        // Act
        exporter.appendMetrics(snapshot);
        exporter.close();

        // Assert - Verify CSV can be parsed correctly
        File csvFile = new File(exporter.getCurrentRunDirectory(), "metrics.csv");
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            reader.readLine(); // Skip header
            String dataLine = reader.readLine();

            // Split by comma
            String[] values = dataLine.split(",");

            assertEquals(5, values.length, "Should have exactly 5 columns");

            // Parse values to verify they're valid numbers
            double timestamp = Double.parseDouble(values[0]);
            double cpuUtil = Double.parseDouble(values[1]);
            double memUtil = Double.parseDouble(values[2]);
            int overloaded = Integer.parseInt(values[3]);
            int migrations = Integer.parseInt(values[4]);

            // Verify parsed values match expected
            assertEquals(15.0, timestamp, 0.01, "Timestamp should parse correctly");
            assertEquals(0.7825, cpuUtil, 0.0001, "CPU utilization should parse correctly");
            assertEquals(0.6543, memUtil, 0.0001, "Memory utilization should parse correctly");
            assertEquals(4, overloaded, "Overloaded count should parse correctly");
            assertEquals(7, migrations, "Migrations count should parse correctly");

            // Verify no quotes around numeric fields
            assertFalse(dataLine.contains("\""), "CSV should not contain quotes around numeric fields");
        }
    }

    @Test
    void testClosePreventsFurtherWrites() throws IOException {
        // Arrange
        exporter.initializeRun("test_close");
        MetricsSnapshot snapshot = createSnapshot(10.0, 0.5, 0.5, 1, 0);

        // Act
        exporter.appendMetrics(snapshot);
        exporter.close();

        // Assert
        MetricsSnapshot snapshot2 = createSnapshot(20.0, 0.6, 0.6, 2, 1);
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> exporter.appendMetrics(snapshot2),
            "Should throw exception when appending after close");

        assertTrue(exception.getMessage().contains("closed"),
            "Exception message should indicate exporter is closed");
    }

    @Test
    void testResourceCleanupInExceptionScenarios() throws IOException {
        // Arrange
        exporter.initializeRun("test_cleanup");

        // Act - Trigger exception with null snapshot
        assertThrows(IllegalArgumentException.class,
            () -> exporter.appendMetrics(null),
            "Should throw exception for null snapshot");

        // Assert - close() should still work after exception
        assertDoesNotThrow(() -> exporter.close(),
            "close() should work even after exceptions");

        // Verify file was created and can be read
        File csvFile = new File(exporter.getCurrentRunDirectory(), "metrics.csv");
        assertTrue(csvFile.exists(), "CSV file should exist even after exception");
    }

    @Test
    void testPathValidation() throws IOException {
        // Arrange - Attempt directory traversal attack
        String maliciousScenarioId = "../../../etc/passwd";

        // Act
        exporter.initializeRun(maliciousScenarioId);

        // Assert - Path should be sanitized
        File runDir = exporter.getCurrentRunDirectory();
        String path = runDir.getAbsolutePath();

        // Sanitized path should not contain ".."
        assertFalse(path.contains(".."), "Sanitized path should not contain '..'");
        assertFalse(path.contains("etc/passwd"), "Sanitized path should not contain 'etc/passwd'");

        // Verify directory is created within temp directory
        assertTrue(path.startsWith(tempDir.toString()),
            "Run directory should be within test temp directory");
    }

    @Test
    void testMultipleCallsToClose() throws IOException {
        // Arrange
        exporter.initializeRun("test_multiple_close");

        // Act & Assert - Multiple calls to close() should be safe
        assertDoesNotThrow(() -> exporter.close(), "First close should succeed");
        assertDoesNotThrow(() -> exporter.close(), "Second close should be safe");
        assertDoesNotThrow(() -> exporter.close(), "Third close should be safe");
    }

    @Test
    void testAppendMetricsBeforeInitialize() {
        // Arrange - Don't call initializeRun()
        MetricsSnapshot snapshot = createSnapshot(10.0, 0.5, 0.5, 1, 0);

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> exporter.appendMetrics(snapshot),
            "Should throw exception when appending before initialize");

        assertTrue(exception.getMessage().contains("not initialized"),
            "Exception should indicate exporter not initialized");
    }

    /**
     * Helper method to create MetricsSnapshot for testing.
     */
    private MetricsSnapshot createSnapshot(double timestamp, double cpu, double mem,
                                          int overloaded, int migrations) {
        return new MetricsSnapshot(timestamp, cpu, mem, overloaded, 0, migrations, 100, 0.0);
    }
}
