package com.cloudsimulation.models;

/**
 * Represents a failed scenario execution during benchmark suite runs.
 * Tracks failures from crashes, timeouts, or YAML parsing errors.
 */
public class FailedScenarioExecution {
    private final String scenarioId;
    private final String algorithmName;
    private final String failureType;  // CRASH, TIMEOUT, YAML_PARSE_ERROR
    private final String errorMessage;
    private final long timestampMs;

    /**
     * Creates a new FailedScenarioExecution record.
     *
     * @param scenarioId Scenario identifier (e.g., "steady_load")
     * @param algorithmName Algorithm that was being tested (e.g., "threshold")
     * @param failureType Type of failure: CRASH, TIMEOUT, or YAML_PARSE_ERROR
     * @param errorMessage Exception message or stack trace
     * @param timestampMs System timestamp when failure occurred
     */
    public FailedScenarioExecution(String scenarioId, String algorithmName,
                                   String failureType, String errorMessage,
                                   long timestampMs) {
        this.scenarioId = scenarioId;
        this.algorithmName = algorithmName;
        this.failureType = failureType;
        this.errorMessage = errorMessage;
        this.timestampMs = timestampMs;
    }

    /**
     * Gets the scenario identifier.
     *
     * @return Scenario ID (e.g., "steady_load", "traffic_spike")
     */
    public String getScenarioId() {
        return scenarioId;
    }

    /**
     * Gets the algorithm name.
     *
     * @return Algorithm identifier (e.g., "threshold", "nsga2")
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Gets the failure type.
     *
     * @return CRASH, TIMEOUT, or YAML_PARSE_ERROR
     */
    public String getFailureType() {
        return failureType;
    }

    /**
     * Gets the error message.
     *
     * @return Exception message or stack trace
     */
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Gets the failure timestamp.
     *
     * @return System timestamp in milliseconds
     */
    public long getTimestampMs() {
        return timestampMs;
    }

    @Override
    public String toString() {
        return String.format(
            "FailedScenarioExecution{scenario='%s', algorithm='%s', type=%s, error='%s'}",
            scenarioId, algorithmName, failureType, errorMessage
        );
    }
}
