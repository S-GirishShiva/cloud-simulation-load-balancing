package com.cloudsimulation.benchmarks.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents the results of a single benchmark execution.
 * Contains timing, memory, and custom metrics collected during benchmark run.
 */
public class BenchmarkResult {
    private final String benchmarkName;
    private final LocalDateTime timestamp;
    private final Map<String, Double> metrics;
    private final Map<String, String> metadata;
    private final String profile;
    private final boolean passed;
    private final String failureReason;

    private BenchmarkResult(Builder builder) {
        this.benchmarkName = builder.benchmarkName;
        this.timestamp = builder.timestamp;
        this.metrics = builder.metrics;
        this.metadata = builder.metadata;
        this.profile = builder.profile;
        this.passed = builder.passed;
        this.failureReason = builder.failureReason;
    }

    public String getBenchmarkName() {
        return benchmarkName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Map<String, Double> getMetrics() {
        return new HashMap<>(metrics);
    }

    public double getMetric(String name) {
        return metrics.getOrDefault(name, 0.0);
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    public String getProfile() {
        return profile;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getFailureReason() {
        return failureReason;
    }

    /**
     * Format timestamp as ISO 8601 string for CSV export
     */
    public String getTimestampISO() {
        return timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }

    @Override
    public String toString() {
        return String.format("BenchmarkResult{name='%s', timestamp=%s, metrics=%d, passed=%b}",
                benchmarkName, getTimestampISO(), metrics.size(), passed);
    }

    /**
     * Builder for constructing BenchmarkResult objects
     */
    public static class Builder {
        private final String benchmarkName;
        private LocalDateTime timestamp = LocalDateTime.now();
        private Map<String, Double> metrics = new HashMap<>();
        private Map<String, String> metadata = new HashMap<>();
        private String profile = "default";
        private boolean passed = true;
        private String failureReason = null;

        public Builder(String benchmarkName) {
            this.benchmarkName = benchmarkName;
        }

        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder addMetric(String name, double value) {
            this.metrics.put(name, value);
            return this;
        }

        public Builder addMetrics(Map<String, Double> metrics) {
            this.metrics.putAll(metrics);
            return this;
        }

        public Builder addMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }

        public Builder profile(String profile) {
            this.profile = profile;
            return this;
        }

        public Builder passed(boolean passed) {
            this.passed = passed;
            return this;
        }

        public Builder failureReason(String reason) {
            this.failureReason = reason;
            this.passed = false;
            return this;
        }

        public BenchmarkResult build() {
            return new BenchmarkResult(this);
        }
    }
}
