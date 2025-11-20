package com.cloudsimulation.models;

import java.util.List;
import java.util.Map;

/**
 * Results from executing scenario benchmark suite across multiple scenarios and algorithms.
 * Contains per-scenario comparison reports and failure tracking.
 */
public class ScenarioBenchmarkResult {
    private final Map<String, ComparisonReport> scenarioReports;
    private final List<FailedScenarioExecution> failedExecutions;
    private final int totalScenariosRun;
    private final int totalAlgorithmsTested;

    /**
     * Creates a new ScenarioBenchmarkResult.
     *
     * @param scenarioReports Map of scenario ID to comparison report
     * @param failedExecutions List of failed scenario executions
     * @param totalScenariosRun Total number of scenarios attempted
     * @param totalAlgorithmsTested Total number of algorithms tested
     */
    public ScenarioBenchmarkResult(
            Map<String, ComparisonReport> scenarioReports,
            List<FailedScenarioExecution> failedExecutions,
            int totalScenariosRun,
            int totalAlgorithmsTested) {
        this.scenarioReports = scenarioReports;
        this.failedExecutions = failedExecutions;
        this.totalScenariosRun = totalScenariosRun;
        this.totalAlgorithmsTested = totalAlgorithmsTested;
    }

    /**
     * Gets the scenario comparison reports.
     *
     * @return Map of scenario ID to ComparisonReport
     */
    public Map<String, ComparisonReport> getScenarioReports() {
        return scenarioReports;
    }

    /**
     * Gets the list of failed scenario executions.
     *
     * @return List of FailedScenarioExecution records
     */
    public List<FailedScenarioExecution> getFailedExecutions() {
        return failedExecutions;
    }

    /**
     * Gets the total number of scenarios run.
     *
     * @return Count of scenario files attempted
     */
    public int getTotalScenariosRun() {
        return totalScenariosRun;
    }

    /**
     * Gets the total number of algorithms tested.
     *
     * @return Count of algorithms in test suite
     */
    public int getTotalAlgorithmsTested() {
        return totalAlgorithmsTested;
    }

    @Override
    public String toString() {
        return String.format(
            "ScenarioBenchmarkResult{scenarios=%d, algorithms=%d, failures=%d}",
            totalScenariosRun, totalAlgorithmsTested, failedExecutions.size()
        );
    }
}
