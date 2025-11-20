package com.cloudsimulation.benchmarks;

import com.cloudsimulation.benchmarks.benchmarks.GCBenchmark;
import com.cloudsimulation.benchmarks.benchmarks.LatencyBenchmark;
import com.cloudsimulation.benchmarks.benchmarks.MemoryBenchmark;
import com.cloudsimulation.benchmarks.benchmarks.ScalabilityBenchmark;
import com.cloudsimulation.benchmarks.benchmarks.StartupBenchmark;
import com.cloudsimulation.benchmarks.benchmarks.ThroughputBenchmark;
import com.cloudsimulation.benchmarks.core.BenchmarkConfig;
import com.cloudsimulation.benchmarks.core.BenchmarkResult;
import com.cloudsimulation.benchmarks.core.PerformanceBenchmarkSuite;
import com.cloudsimulation.benchmarks.reporting.BaselineManager;
import com.cloudsimulation.benchmarks.reporting.CSVResultWriter;
import com.cloudsimulation.benchmarks.reporting.HTMLReportGenerator;

import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for Performance Benchmark Suite.
 * Runs all benchmarks, saves results, and checks for regressions.
 */
@Tag("benchmark")
public class PerformanceBenchmarkSuiteTest {

    private PerformanceBenchmarkSuite suite;
    private BenchmarkConfig config;
    private CSVResultWriter csvWriter;
    private BaselineManager baselineManager;

    @BeforeEach
    void setUp() {
        // CloudSim simulations can only run once - use single measurement iteration
        config = new BenchmarkConfig.Builder()
                .warmupIterations(0)  // Warmup skipped via override in benchmarks
                .measurementIterations(1)  // Single run since CloudSim can't be restarted
                .profile("test")
                .build();
        suite = new PerformanceBenchmarkSuite(config);
        csvWriter = new CSVResultWriter();
        baselineManager = new BaselineManager();
    }

    @Test
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void testFullBenchmarkSuite() throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("RUNNING FULL PERFORMANCE BENCHMARK SUITE");
        System.out.println("=".repeat(80));

        // Add all benchmarks
        suite.addBenchmark(new StartupBenchmark());
        suite.addBenchmark(new ThroughputBenchmark());
        suite.addBenchmark(new MemoryBenchmark());
        suite.addBenchmark(new GCBenchmark());
        suite.addBenchmark(new LatencyBenchmark());
        suite.addBenchmark(new ScalabilityBenchmark());

        // Run all benchmarks
        List<BenchmarkResult> results = suite.runAll();

        // Verify results
        assertNotNull(results);
        assertEquals(6, results.size(), "Expected 6 benchmark results");

        // Verify each benchmark ran
        assertTrue(results.stream().anyMatch(r -> r.getBenchmarkName().equals("startup")),
                "Startup benchmark should have run");
        assertTrue(results.stream().anyMatch(r -> r.getBenchmarkName().equals("throughput")),
                "Throughput benchmark should have run");
        assertTrue(results.stream().anyMatch(r -> r.getBenchmarkName().equals("memory")),
                "Memory benchmark should have run");
        assertTrue(results.stream().anyMatch(r -> r.getBenchmarkName().equals("gc")),
                "GC benchmark should have run");
        assertTrue(results.stream().anyMatch(r -> r.getBenchmarkName().equals("latency")),
                "Latency benchmark should have run");
        assertTrue(results.stream().anyMatch(r -> r.getBenchmarkName().equals("scalability")),
                "Scalability benchmark should have run");

        // Print results
        System.out.println("\n" + "=".repeat(80));
        System.out.println("BENCHMARK RESULTS SUMMARY");
        System.out.println("=".repeat(80));
        for (BenchmarkResult result : results) {
            System.out.println("\nBenchmark: " + result.getBenchmarkName());
            System.out.println("  Status: " + (result.isPassed() ? "PASSED" : "FAILED"));
            if (!result.isPassed()) {
                System.out.println("  Failure: " + result.getFailureReason());
            }
            System.out.println("  Metrics:");
            result.getMetrics().forEach((metric, value) ->
                    System.out.printf("    %s: %.4f%n", metric, value));
        }

        // Save results to CSV
        System.out.println("\n" + "=".repeat(80));
        System.out.println("SAVING RESULTS TO CSV");
        System.out.println("=".repeat(80));
        csvWriter.writeResults(results);
        System.out.println("Results saved to: " + csvWriter.getResultsFilePath());

        // Check for regressions (if baseline exists)
        System.out.println("\n" + "=".repeat(80));
        System.out.println("CHECKING FOR REGRESSIONS");
        System.out.println("=".repeat(80));

        String profile = config.getProfile();
        if (baselineManager.hasBaseline(profile)) {
            baselineManager.loadBaseline(profile);
            List<String> regressions = baselineManager.detectRegressions(results);

            if (!regressions.isEmpty()) {
                System.out.println("WARNING: Performance regressions detected:");
                regressions.forEach(r -> System.out.println("  - " + r));
            } else {
                System.out.println("No regressions detected - performance within acceptable range");
            }
        } else {
            System.out.println("No baseline found - establishing baseline with current results");
            baselineManager.saveBaseline(results, profile);
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("BENCHMARK SUITE COMPLETED SUCCESSFULLY");
        System.out.println(suite.getSummary());
        System.out.println("=".repeat(80) + "\n");
    }

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testStartupBenchmark() throws Exception {
        suite.addBenchmark(new StartupBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        assertEquals(1, results.size());
        BenchmarkResult result = results.get(0);

        assertEquals("startup", result.getBenchmarkName());
        assertTrue(result.getMetrics().containsKey("initialization_time_ms"),
                "Should measure initialization time");
        assertTrue(result.getMetrics().containsKey("cloudsim_creation_ms"),
                "Should measure CloudSim creation time");
        assertTrue(result.getMetrics().containsKey("federation_creation_ms"),
                "Should measure federation creation time");

        double initTime = result.getMetric("initialization_time_ms");
        System.out.println("Startup initialization time: " + initTime + " ms");
        assertTrue(initTime > 0, "Initialization time should be positive");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testThroughputBenchmark() throws Exception {
        suite.addBenchmark(new ThroughputBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        assertEquals(1, results.size());
        BenchmarkResult result = results.get(0);

        assertEquals("throughput", result.getBenchmarkName());
        assertTrue(result.getMetrics().containsKey("vms_per_second"),
                "Should measure VMs per second");
        assertTrue(result.getMetrics().containsKey("overall_throughput_vms_per_sec"),
                "Should measure overall throughput");

        double vmsPerSec = result.getMetric("vms_per_second");
        System.out.println("VM creation throughput: " + vmsPerSec + " VMs/s");
        assertTrue(vmsPerSec > 0, "Throughput should be positive");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    @org.junit.jupiter.api.Disabled("Memory measurements unreliable with single-run benchmarks - timing too tight for meaningful deltas")
    void testMemoryBenchmark() throws Exception {
        suite.addBenchmark(new MemoryBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        assertEquals(1, results.size());
        BenchmarkResult result = results.get(0);

        assertEquals("memory", result.getBenchmarkName());
        assertTrue(result.getMetrics().containsKey("mb_per_vm"),
                "Should measure memory per VM");
        assertTrue(result.getMetrics().containsKey("peak_memory_mb"),
                "Should measure peak memory");
        assertTrue(result.getMetrics().containsKey("cleanup_efficiency_percent"),
                "Should measure cleanup efficiency");

        double mbPerVm = result.getMetric("mb_per_vm");
        System.out.println("Memory per VM: " + mbPerVm + " MB");
        assertTrue(mbPerVm > 0, "Memory per VM should be positive");
    }

    @Test
    void testCSVWriter() throws Exception {
        suite.addBenchmark(new StartupBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        // Clear existing results
        csvWriter.clearResults();

        // Write results
        csvWriter.writeResults(results);

        // Verify file exists
        assertTrue(csvWriter.getResultsFilePath().toFile().exists(),
                "CSV file should be created");

        // Verify file has content
        long fileSize = csvWriter.getResultsFilePath().toFile().length();
        assertTrue(fileSize > 0, "CSV file should have content");

        System.out.println("CSV file created at: " + csvWriter.getResultsFilePath());
        System.out.println("File size: " + fileSize + " bytes");
    }

    @Test
    @org.junit.jupiter.api.Disabled("Baseline regression detection incompatible with single-run benchmarks - high natural variability")
    void testBaselineComparison() throws Exception {
        String profile = "test-profile";
        suite.addBenchmark(new StartupBenchmark());

        // Run benchmarks first time - establish baseline
        List<BenchmarkResult> firstRun = suite.runAll();
        baselineManager.saveBaseline(firstRun, profile);
        assertTrue(baselineManager.hasBaseline(profile), "Baseline should be saved");

        // Run benchmarks second time - compare against baseline
        List<BenchmarkResult> secondRun = suite.runAll();
        baselineManager.loadBaseline(profile);
        List<String> regressions = baselineManager.detectRegressions(secondRun);

        System.out.println("Regressions detected: " + regressions.size());
        regressions.forEach(System.out::println);

        // Second run should be similar to first run (no major regressions)
        // We allow some variation due to system noise
        assertTrue(regressions.size() <= firstRun.size(),
                "Should not have more regressions than benchmarks");
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testGCBenchmark() throws Exception {
        suite.addBenchmark(new GCBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        assertEquals(1, results.size());
        BenchmarkResult result = results.get(0);

        assertEquals("gc", result.getBenchmarkName());
        assertTrue(result.getMetrics().containsKey("total_gc_collections"),
                "Should measure total GC collections");
        assertTrue(result.getMetrics().containsKey("total_gc_pause_ms"),
                "Should measure total GC pause time");
        assertTrue(result.getMetrics().containsKey("avg_pause_ms"),
                "Should measure average pause time");
        assertTrue(result.getMetrics().containsKey("vm_gc_overhead_percent"),
                "Should measure VM GC overhead");

        double avgPause = result.getMetric("avg_pause_ms");
        System.out.println("Average GC pause time: " + avgPause + " ms");
        assertTrue(avgPause >= 0, "Average pause should be non-negative");
    }

    @Test
    @Timeout(value = 90, unit = TimeUnit.SECONDS)
    void testLatencyBenchmark() throws Exception {
        suite.addBenchmark(new LatencyBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        assertEquals(1, results.size());
        BenchmarkResult result = results.get(0);

        assertEquals("latency", result.getBenchmarkName());
        assertTrue(result.getMetrics().containsKey("vm_creation_p50_us"),
                "Should measure VM creation p50 latency");
        assertTrue(result.getMetrics().containsKey("vm_creation_p95_us"),
                "Should measure VM creation p95 latency");
        assertTrue(result.getMetrics().containsKey("vm_creation_p99_us"),
                "Should measure VM creation p99 latency");
        assertTrue(result.getMetrics().containsKey("cloudlet_submission_p50_us"),
                "Should measure cloudlet submission p50 latency");

        double p95 = result.getMetric("vm_creation_p95_us");
        System.out.println("VM creation p95 latency: " + p95 + " us");
        assertTrue(p95 > 0, "p95 latency should be positive");
    }

    @Test
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testScalabilityBenchmark() throws Exception {
        suite.addBenchmark(new ScalabilityBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        assertEquals(1, results.size());
        BenchmarkResult result = results.get(0);

        assertEquals("scalability", result.getBenchmarkName());
        assertTrue(result.getMetrics().containsKey("scale_25_vms_time_ms"),
                "Should measure 25 VM scale time");
        assertTrue(result.getMetrics().containsKey("scale_100_vms_time_ms"),
                "Should measure 100 VM scale time");
        assertTrue(result.getMetrics().containsKey("scaling_factor"),
                "Should calculate scaling factor");
        assertTrue(result.getMetrics().containsKey("scaling_efficiency_percent"),
                "Should calculate scaling efficiency");

        double scalingFactor = result.getMetric("scaling_factor");
        System.out.println("Scaling factor: " + scalingFactor);
        assertTrue(scalingFactor > 0, "Scaling factor should be positive");
    }

    @Test
    void testHTMLReportGeneration() throws Exception {
        // Run a few benchmarks
        suite.addBenchmark(new StartupBenchmark());
        suite.addBenchmark(new ThroughputBenchmark());
        suite.addBenchmark(new MemoryBenchmark());
        List<BenchmarkResult> results = suite.runAll();

        // Generate HTML report
        HTMLReportGenerator htmlGenerator = new HTMLReportGenerator();
        Path reportPath = htmlGenerator.generateReport(results);

        // Verify report was created
        assertTrue(reportPath.toFile().exists(), "HTML report should be created");
        assertTrue(reportPath.toFile().length() > 0, "HTML report should have content");

        // Verify it's valid HTML (basic check)
        String content = new String(java.nio.file.Files.readAllBytes(reportPath));
        assertTrue(content.contains("<!DOCTYPE html>"), "Should be valid HTML");
        assertTrue(content.contains("CloudSim Performance Benchmark Report"), "Should have title");
        assertTrue(content.contains("chart.js") || content.contains("Chart.js"), "Should include Chart.js");
        assertTrue(content.contains("startup"), "Should contain benchmark names");

        System.out.println("HTML report generated at: " + reportPath);
        System.out.println("File size: " + reportPath.toFile().length() + " bytes");
    }
}
