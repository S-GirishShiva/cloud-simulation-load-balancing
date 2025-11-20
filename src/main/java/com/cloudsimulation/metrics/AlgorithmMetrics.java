package com.cloudsimulation.metrics;

/**
 * Captures algorithm-specific performance metrics for a single evaluation cycle or aggregated simulation.
 * Immutable class constructed via Builder pattern.
 */
public class AlgorithmMetrics {
    private final String algorithmName;
    private final double timestamp;
    private final int totalMigrations;
    private final int overloadEvents;
    private final double averageUtilization;
    private final int slaViolations;
    private final long decisionTimeMs;

    /**
     * Private constructor - use Builder to construct instances.
     *
     * @param builder Builder instance with configured values
     */
    private AlgorithmMetrics(Builder builder) {
        this.algorithmName = builder.algorithmName;
        this.timestamp = builder.timestamp;
        this.totalMigrations = builder.totalMigrations;
        this.overloadEvents = builder.overloadEvents;
        this.averageUtilization = builder.averageUtilization;
        this.slaViolations = builder.slaViolations;
        this.decisionTimeMs = builder.decisionTimeMs;
    }

    /**
     * Gets the algorithm identifier.
     *
     * @return Algorithm name (e.g., "threshold", "nsga2", "hybrid")
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Gets the simulation timestamp.
     *
     * @return Simulation time in seconds
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the total number of migrations.
     *
     * @return Migration count (cumulative or per-cycle depending on context)
     */
    public int getTotalMigrations() {
        return totalMigrations;
    }

    /**
     * Gets the count of overload events.
     *
     * @return Number of overload threshold breaches
     */
    public int getOverloadEvents() {
        return overloadEvents;
    }

    /**
     * Gets the average CPU utilization.
     *
     * @return Mean CPU utilization (0-1 range)
     */
    public double getAverageUtilization() {
        return averageUtilization;
    }

    /**
     * Gets the count of SLA violations.
     *
     * @return Number of SLA violations
     */
    public int getSlaViolations() {
        return slaViolations;
    }

    /**
     * Gets the decision computation time.
     *
     * @return Milliseconds to compute algorithm decision
     */
    public long getDecisionTimeMs() {
        return decisionTimeMs;
    }

    @Override
    public String toString() {
        return String.format(
            "AlgorithmMetrics{algorithm='%s', timestamp=%.2f, migrations=%d, overloads=%d, utilization=%.2f, slaViolations=%d, decisionTime=%dms}",
            algorithmName, timestamp, totalMigrations, overloadEvents, averageUtilization, slaViolations, decisionTimeMs
        );
    }

    /**
     * Builder for constructing AlgorithmMetrics instances.
     * Provides flexible construction with default values.
     */
    public static class Builder {
        private String algorithmName = "";
        private double timestamp = 0.0;
        private int totalMigrations = 0;
        private int overloadEvents = 0;
        private double averageUtilization = 0.0;
        private int slaViolations = 0;
        private long decisionTimeMs = 0L;

        /**
         * Creates a new Builder with default values.
         */
        public Builder() {
        }

        /**
         * Sets the algorithm name.
         *
         * @param algorithmName Algorithm identifier
         * @return This builder
         */
        public Builder algorithmName(String algorithmName) {
            this.algorithmName = algorithmName;
            return this;
        }

        /**
         * Sets the timestamp.
         *
         * @param timestamp Simulation time in seconds
         * @return This builder
         */
        public Builder timestamp(double timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        /**
         * Sets the total migrations count.
         *
         * @param totalMigrations Migration count
         * @return This builder
         */
        public Builder totalMigrations(int totalMigrations) {
            this.totalMigrations = totalMigrations;
            return this;
        }

        /**
         * Sets the overload events count.
         *
         * @param overloadEvents Overload event count
         * @return This builder
         */
        public Builder overloadEvents(int overloadEvents) {
            this.overloadEvents = overloadEvents;
            return this;
        }

        /**
         * Sets the average utilization.
         *
         * @param averageUtilization Average CPU utilization (0-1 range)
         * @return This builder
         */
        public Builder averageUtilization(double averageUtilization) {
            this.averageUtilization = averageUtilization;
            return this;
        }

        /**
         * Sets the SLA violations count.
         *
         * @param slaViolations SLA violation count
         * @return This builder
         */
        public Builder slaViolations(int slaViolations) {
            this.slaViolations = slaViolations;
            return this;
        }

        /**
         * Sets the decision time.
         *
         * @param decisionTimeMs Computation time in milliseconds
         * @return This builder
         */
        public Builder decisionTimeMs(long decisionTimeMs) {
            this.decisionTimeMs = decisionTimeMs;
            return this;
        }

        /**
         * Builds the AlgorithmMetrics instance.
         *
         * @return Immutable AlgorithmMetrics object
         */
        public AlgorithmMetrics build() {
            return new AlgorithmMetrics(this);
        }
    }
}
