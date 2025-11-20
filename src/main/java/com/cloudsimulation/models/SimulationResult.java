package com.cloudsimulation.models;

import com.cloudsimulation.metrics.AlgorithmMetrics;

/**
 * Encapsulates the results of a single simulation run.
 * Contains aggregated algorithm metrics and execution metadata.
 */
public class SimulationResult {
    private final String scenarioId;
    private final AlgorithmMetrics algorithmMetrics;
    private final int cloudletsProcessed;
    private final long memoryUsedMB;
    private final boolean success;

    /**
     * Creates a new SimulationResult.
     *
     * @param scenarioId Scenario identifier
     * @param algorithmMetrics Aggregated algorithm performance metrics
     * @param cloudletsProcessed Number of cloudlets completed
     * @param memoryUsedMB Memory consumption in megabytes
     * @param success Whether simulation completed successfully
     */
    public SimulationResult(String scenarioId, AlgorithmMetrics algorithmMetrics,
                           int cloudletsProcessed, long memoryUsedMB, boolean success) {
        this.scenarioId = scenarioId;
        this.algorithmMetrics = algorithmMetrics;
        this.cloudletsProcessed = cloudletsProcessed;
        this.memoryUsedMB = memoryUsedMB;
        this.success = success;
    }

    /**
     * Gets the scenario identifier.
     *
     * @return Scenario ID
     */
    public String getScenarioId() {
        return scenarioId;
    }

    /**
     * Gets the aggregated algorithm metrics.
     *
     * @return AlgorithmMetrics from this simulation run
     */
    public AlgorithmMetrics getAlgorithmMetrics() {
        return algorithmMetrics;
    }

    /**
     * Gets the number of cloudlets processed.
     *
     * @return Count of completed cloudlets
     */
    public int getCloudletsProcessed() {
        return cloudletsProcessed;
    }

    /**
     * Gets the memory usage.
     *
     * @return Memory consumption in MB
     */
    public long getMemoryUsedMB() {
        return memoryUsedMB;
    }

    /**
     * Checks if simulation completed successfully.
     *
     * @return true if successful, false if failed
     */
    public boolean isSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return String.format(
            "SimulationResult{scenario='%s', cloudlets=%d, memoryMB=%d, success=%b}",
            scenarioId, cloudletsProcessed, memoryUsedMB, success
        );
    }
}
