package com.cloudsimulation.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents a load balancing algorithm's output for a given tick.
 * Contains all planned migrations and associated metadata.
 * Immutable class constructed via Builder pattern.
 */
public class LoadBalancingPlan {
    private final String decisionId;
    private final double timestamp;
    private final String algorithmName;
    private final List<MigrationAction> migrations;
    private final long computationTime;

    /**
     * Private constructor - use Builder to construct instances.
     */
    private LoadBalancingPlan(Builder builder) {
        this.decisionId = builder.decisionId;
        this.timestamp = builder.timestamp;
        this.algorithmName = builder.algorithmName;
        this.migrations = Collections.unmodifiableList(new ArrayList<>(builder.migrations));
        this.computationTime = builder.computationTime;
    }

    /**
     * Gets the unique decision identifier.
     *
     * @return Decision ID
     */
    public String getDecisionId() {
        return decisionId;
    }

    /**
     * Gets the simulation timestamp when decision was made.
     *
     * @return Timestamp in seconds
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the algorithm name that generated this plan.
     *
     * @return Algorithm identifier (e.g., "threshold", "nsga2", "hybrid")
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Gets the list of planned migrations.
     * Returns an immutable list.
     *
     * @return List of MigrationAction objects (empty list if no migrations)
     */
    public List<MigrationAction> getMigrations() {
        return migrations;
    }

    /**
     * Gets the computation time for this decision.
     *
     * @return Milliseconds to compute decision
     */
    public long getComputationTime() {
        return computationTime;
    }

    /**
     * Gets the number of migrations in this plan.
     *
     * @return Migration count
     */
    public int getMigrationCount() {
        return migrations.size();
    }

    @Override
    public String toString() {
        return String.format(
            "LoadBalancingPlan{id='%s', timestamp=%.2f, algorithm='%s', migrations=%d, computationTime=%dms}",
            decisionId, timestamp, algorithmName, migrations.size(), computationTime
        );
    }

    /**
     * Builder for constructing LoadBalancingPlan instances.
     * Provides flexible construction with sensible defaults.
     */
    public static class Builder {
        private String decisionId;
        private double timestamp;
        private String algorithmName;
        private List<MigrationAction> migrations;
        private long computationTime;

        /**
         * Creates a new Builder with default values.
         */
        public Builder() {
            this.decisionId = UUID.randomUUID().toString();
            this.timestamp = 0.0;
            this.algorithmName = "unknown";
            this.migrations = new ArrayList<>();
            this.computationTime = 0L;
        }

        /**
         * Sets the decision ID.
         *
         * @param decisionId Unique identifier
         * @return This builder
         */
        public Builder decisionId(String decisionId) {
            this.decisionId = decisionId;
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
         * Sets the list of migrations.
         *
         * @param migrations List of MigrationAction objects
         * @return This builder
         */
        public Builder migrations(List<MigrationAction> migrations) {
            this.migrations = new ArrayList<>(migrations);
            return this;
        }

        /**
         * Adds a single migration to the plan.
         *
         * @param migration MigrationAction to add
         * @return This builder
         */
        public Builder addMigration(MigrationAction migration) {
            this.migrations.add(migration);
            return this;
        }

        /**
         * Sets the computation time.
         *
         * @param computationTime Milliseconds to compute decision
         * @return This builder
         */
        public Builder computationTime(long computationTime) {
            this.computationTime = computationTime;
            return this;
        }

        /**
         * Builds the LoadBalancingPlan instance.
         *
         * @return Immutable LoadBalancingPlan
         */
        public LoadBalancingPlan build() {
            return new LoadBalancingPlan(this);
        }
    }
}
