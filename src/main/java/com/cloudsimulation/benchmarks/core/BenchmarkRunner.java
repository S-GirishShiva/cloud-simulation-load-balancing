package com.cloudsimulation.benchmarks.core;

/**
 * Interface for all benchmark implementations.
 * Each benchmark must implement setup, run, and teardown phases.
 */
public interface BenchmarkRunner {

    /**
     * Get the name of this benchmark
     */
    String getName();

    /**
     * Get a brief description of what this benchmark measures
     */
    String getDescription();

    /**
     * Setup phase - initialize resources before measurement
     * This phase is not timed.
     */
    void setup() throws Exception;

    /**
     * Run the benchmark and collect metrics
     * This phase IS timed and measured.
     *
     * @param config Configuration parameters for this run
     * @return Results of the benchmark execution
     */
    BenchmarkResult run(BenchmarkConfig config) throws Exception;

    /**
     * Teardown phase - cleanup resources after measurement
     * This phase is not timed.
     */
    void teardown() throws Exception;

    /**
     * Warmup phase - run benchmark without measurement to warm up JVM
     * Default implementation runs the benchmark once and discards results.
     */
    default void warmup(BenchmarkConfig config) throws Exception {
        for (int i = 0; i < config.getWarmupIterations(); i++) {
            run(config);
        }
    }
}
