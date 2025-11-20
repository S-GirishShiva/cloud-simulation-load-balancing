package com.cloudsimulation.benchmarks.core;

/**
 * Configuration parameters for benchmark execution.
 * Controls warmup, measurement iterations, and resource limits.
 */
public class BenchmarkConfig {
    private final int warmupIterations;
    private final int measurementIterations;
    private final long warmupDurationMs;
    private final long measurementDurationMs;
    private final boolean enableGCLogging;
    private final String profile;
    private final String resultsDirectory;

    private BenchmarkConfig(Builder builder) {
        this.warmupIterations = builder.warmupIterations;
        this.measurementIterations = builder.measurementIterations;
        this.warmupDurationMs = builder.warmupDurationMs;
        this.measurementDurationMs = builder.measurementDurationMs;
        this.enableGCLogging = builder.enableGCLogging;
        this.profile = builder.profile;
        this.resultsDirectory = builder.resultsDirectory;
    }

    public int getWarmupIterations() {
        return warmupIterations;
    }

    public int getMeasurementIterations() {
        return measurementIterations;
    }

    public long getWarmupDurationMs() {
        return warmupDurationMs;
    }

    public long getMeasurementDurationMs() {
        return measurementDurationMs;
    }

    public boolean isEnableGCLogging() {
        return enableGCLogging;
    }

    public String getProfile() {
        return profile;
    }

    public String getResultsDirectory() {
        return resultsDirectory;
    }

    /**
     * Create default configuration for quick benchmarks
     */
    public static BenchmarkConfig quick() {
        return new Builder()
                .warmupIterations(1)
                .measurementIterations(3)
                .build();
    }

    /**
     * Create standard configuration for comprehensive benchmarks
     */
    public static BenchmarkConfig standard() {
        return new Builder()
                .warmupIterations(3)
                .measurementIterations(5)
                .build();
    }

    /**
     * Create configuration optimized for CI/CD environments
     */
    public static BenchmarkConfig ci() {
        return new Builder()
                .warmupIterations(2)
                .measurementIterations(3)
                .enableGCLogging(false)
                .build();
    }

    @Override
    public String toString() {
        return String.format("BenchmarkConfig{warmup=%d, measurement=%d, profile=%s}",
                warmupIterations, measurementIterations, profile);
    }

    /**
     * Builder for constructing BenchmarkConfig objects
     */
    public static class Builder {
        private int warmupIterations = 2;
        private int measurementIterations = 5;
        private long warmupDurationMs = 1000;
        private long measurementDurationMs = 2000;
        private boolean enableGCLogging = false;
        private String profile = "default";
        private String resultsDirectory = "target/benchmarks";

        public Builder warmupIterations(int iterations) {
            this.warmupIterations = iterations;
            return this;
        }

        public Builder measurementIterations(int iterations) {
            this.measurementIterations = iterations;
            return this;
        }

        public Builder warmupDurationMs(long durationMs) {
            this.warmupDurationMs = durationMs;
            return this;
        }

        public Builder measurementDurationMs(long durationMs) {
            this.measurementDurationMs = durationMs;
            return this;
        }

        public Builder enableGCLogging(boolean enable) {
            this.enableGCLogging = enable;
            return this;
        }

        public Builder profile(String profile) {
            this.profile = profile;
            return this;
        }

        public Builder resultsDirectory(String directory) {
            this.resultsDirectory = directory;
            return this;
        }

        public BenchmarkConfig build() {
            return new BenchmarkConfig(this);
        }
    }
}
