package com.cloudsimulation.benchmarks.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Main orchestrator for running performance benchmarks.
 * Coordinates execution of all benchmarks, collects results, and generates reports.
 *
 * Usage:
 * <pre>
 * PerformanceBenchmarkSuite suite = new PerformanceBenchmarkSuite(BenchmarkConfig.standard());
 * suite.addBenchmark(new StartupBenchmark());
 * suite.addBenchmark(new ThroughputBenchmark());
 * List&lt;BenchmarkResult&gt; results = suite.runAll();
 * </pre>
 */
public class PerformanceBenchmarkSuite {
    private static final Logger logger = LoggerFactory.getLogger(PerformanceBenchmarkSuite.class);

    private final List<BenchmarkRunner> benchmarks;
    private final BenchmarkConfig config;
    private final List<BenchmarkResult> results;

    public PerformanceBenchmarkSuite(BenchmarkConfig config) {
        this.config = config;
        this.benchmarks = new ArrayList<>();
        this.results = new ArrayList<>();
    }

    /**
     * Add a benchmark to the suite
     */
    public void addBenchmark(BenchmarkRunner benchmark) {
        benchmarks.add(benchmark);
    }

    /**
     * Run all benchmarks in the suite
     *
     * @return List of benchmark results
     */
    public List<BenchmarkResult> runAll() {
        logger.info("=".repeat(60));
        logger.info("PERFORMANCE BENCHMARK SUITE STARTED");
        logger.info("Configuration: {}", config);
        logger.info("Benchmarks to run: {}", benchmarks.size());
        logger.info("=".repeat(60));

        results.clear();
        long suiteStartTime = System.nanoTime();

        for (BenchmarkRunner benchmark : benchmarks) {
            logger.info("\n--- Running benchmark: {} ---", benchmark.getName());
            logger.info("Description: {}", benchmark.getDescription());

            try {
                // Setup phase
                logger.info("Setup phase...");
                benchmark.setup();

                // Warmup phase
                logger.info("Warmup phase ({} iterations)...", config.getWarmupIterations());
                benchmark.warmup(config);

                // Measurement phase
                logger.info("Measurement phase ({} iterations)...", config.getMeasurementIterations());
                List<BenchmarkResult> iterationResults = new ArrayList<>();

                for (int i = 0; i < config.getMeasurementIterations(); i++) {
                    logger.info("  Iteration {}/{}...", i + 1, config.getMeasurementIterations());
                    BenchmarkResult result = benchmark.run(config);
                    iterationResults.add(result);
                }

                // Aggregate results from all iterations
                BenchmarkResult aggregatedResult = aggregateResults(
                        benchmark.getName(), iterationResults, config.getProfile());
                results.add(aggregatedResult);

                // Teardown phase
                logger.info("Teardown phase...");
                benchmark.teardown();

                // Report result
                if (aggregatedResult.isPassed()) {
                    logger.info("Result: PASSED");
                } else {
                    logger.warn("Result: FAILED - {}", aggregatedResult.getFailureReason());
                }
                logger.info("Metrics: {}", aggregatedResult.getMetrics());

            } catch (Exception e) {
                logger.error("Benchmark failed with exception: {}", e.getMessage(), e);
                BenchmarkResult failedResult = new BenchmarkResult.Builder(benchmark.getName())
                        .profile(config.getProfile())
                        .failureReason("Exception: " + e.getMessage())
                        .build();
                results.add(failedResult);
            }
        }

        long suiteEndTime = System.nanoTime();
        double suiteDurationMs = (suiteEndTime - suiteStartTime) / 1_000_000.0;

        logger.info("\n" + "=".repeat(60));
        logger.info("BENCHMARK SUITE COMPLETED");
        logger.info("Total duration: {:.2f} ms", suiteDurationMs);
        logger.info("Benchmarks run: {}", results.size());
        long passedCount = results.stream().filter(BenchmarkResult::isPassed).count();
        logger.info("Passed: {} / {}", passedCount, results.size());
        logger.info("=".repeat(60));

        return new ArrayList<>(results);
    }

    /**
     * Run a single benchmark by name
     */
    public BenchmarkResult runSingle(String benchmarkName) {
        for (BenchmarkRunner benchmark : benchmarks) {
            if (benchmark.getName().equals(benchmarkName)) {
                try {
                    benchmark.setup();
                    benchmark.warmup(config);
                    BenchmarkResult result = benchmark.run(config);
                    benchmark.teardown();
                    return result;
                } catch (Exception e) {
                    logger.error("Benchmark {} failed: {}", benchmarkName, e.getMessage(), e);
                    return new BenchmarkResult.Builder(benchmarkName)
                            .failureReason("Exception: " + e.getMessage())
                            .build();
                }
            }
        }
        throw new IllegalArgumentException("Benchmark not found: " + benchmarkName);
    }

    /**
     * Get all results from the last run
     */
    public List<BenchmarkResult> getResults() {
        return new ArrayList<>(results);
    }

    /**
     * Aggregate multiple iteration results into a single result with averaged metrics
     */
    private BenchmarkResult aggregateResults(String benchmarkName, List<BenchmarkResult> results, String profile) {
        BenchmarkResult.Builder aggregated = new BenchmarkResult.Builder(benchmarkName)
                .profile(profile);

        // Check if any iteration failed
        boolean allPassed = results.stream().allMatch(BenchmarkResult::isPassed);
        if (!allPassed) {
            String failures = results.stream()
                    .filter(r -> !r.isPassed())
                    .map(BenchmarkResult::getFailureReason)
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown failures");
            aggregated.failureReason(failures);
        }

        // Average all metrics across iterations
        if (!results.isEmpty()) {
            // Get all unique metric names
            results.get(0).getMetrics().keySet().forEach(metricName -> {
                double sum = results.stream()
                        .mapToDouble(r -> r.getMetric(metricName))
                        .sum();
                double average = sum / results.size();
                aggregated.addMetric(metricName, average);
            });

            // Copy metadata from first result (should be same for all)
            aggregated.addMetadata("iterations", String.valueOf(results.size()));
            results.get(0).getMetadata().forEach(aggregated::addMetadata);
        }

        return aggregated.build();
    }

    /**
     * Get summary statistics of the benchmark suite
     */
    public String getSummary() {
        long total = results.size();
        long passed = results.stream().filter(BenchmarkResult::isPassed).count();
        long failed = total - passed;

        return String.format("Benchmark Summary: %d total, %d passed, %d failed", total, passed, failed);
    }
}
