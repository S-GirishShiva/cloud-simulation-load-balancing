package com.cloudsimulation.models;

import java.util.List;
import java.util.Map;

/**
 * Aggregates performance metrics for algorithm comparison.
 * Contains results for all algorithms compared in a scenario.
 */
public class ComparisonReport {
    private final String scenarioId;
    private final List<AlgorithmResult> algorithmResults;
    private final String baselineAlgorithm;
    private final List<String> paretoFront;
    private final Map<String, StatisticalTest> statisticalTests;

    /**
     * Creates a new ComparisonReport.
     *
     * @param scenarioId Unique identifier for the scenario
     * @param algorithmResults List of results for each algorithm
     * @param baselineAlgorithm Name of the baseline algorithm
     * @param paretoFront List of Pareto-optimal algorithm names
     * @param statisticalTests Map of metric names to statistical test results
     */
    public ComparisonReport(String scenarioId, List<AlgorithmResult> algorithmResults,
                           String baselineAlgorithm, List<String> paretoFront,
                           Map<String, StatisticalTest> statisticalTests) {
        this.scenarioId = scenarioId;
        this.algorithmResults = algorithmResults;
        this.baselineAlgorithm = baselineAlgorithm;
        this.paretoFront = paretoFront;
        this.statisticalTests = statisticalTests;
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
     * Gets the algorithm results.
     *
     * @return List of AlgorithmResult objects
     */
    public List<AlgorithmResult> getAlgorithmResults() {
        return algorithmResults;
    }

    /**
     * Gets the baseline algorithm name.
     *
     * @return Baseline algorithm identifier
     */
    public String getBaselineAlgorithm() {
        return baselineAlgorithm;
    }

    /**
     * Gets the Pareto front algorithm names.
     *
     * @return List of Pareto-optimal algorithm names
     */
    public List<String> getParetoFront() {
        return paretoFront;
    }

    /**
     * Gets the statistical test results.
     *
     * @return Map of metric names to StatisticalTest objects
     */
    public Map<String, StatisticalTest> getStatisticalTests() {
        return statisticalTests;
    }

    @Override
    public String toString() {
        return String.format("ComparisonReport{scenario='%s', algorithms=%d, paretoFront=%s}",
            scenarioId, algorithmResults.size(), paretoFront);
    }
}
