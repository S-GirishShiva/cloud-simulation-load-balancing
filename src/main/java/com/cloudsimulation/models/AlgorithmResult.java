package com.cloudsimulation.models;

import com.cloudsimulation.metrics.AlgorithmMetrics;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents results for a single algorithm with improvement metrics.
 * Tracks performance metrics and calculates percentage improvement over baseline.
 */
public class AlgorithmResult {
    private final String algorithmName;
    private final AlgorithmMetrics aggregatedMetrics;
    private final Map<String, Double> percentageImprovement;
    private final boolean isBaselineAlgorithm;
    private final boolean isParetoOptimal;

    /**
     * Creates a new AlgorithmResult.
     *
     * @param algorithmName Name of the algorithm
     * @param aggregatedMetrics Aggregated metrics for the algorithm
     * @param isBaselineAlgorithm Whether this is the baseline algorithm
     * @param isParetoOptimal Whether this algorithm is Pareto-optimal
     */
    public AlgorithmResult(String algorithmName, AlgorithmMetrics aggregatedMetrics,
                          boolean isBaselineAlgorithm, boolean isParetoOptimal) {
        this.algorithmName = algorithmName;
        this.aggregatedMetrics = aggregatedMetrics;
        this.isBaselineAlgorithm = isBaselineAlgorithm;
        this.isParetoOptimal = isParetoOptimal;
        this.percentageImprovement = new HashMap<>();
    }

    /**
     * Calculates percentage improvement over baseline for all metrics.
     * <p>
     * For minimization objectives (migrations, overloads, SLA violations, decision time):
     * improvement = (baseline - candidate) / baseline * 100
     * Positive % = improvement (candidate < baseline is good)
     * </p>
     * <p>
     * For maximization objectives (utilization):
     * improvement = (candidate - baseline) / baseline * 100
     * Positive % = improvement (candidate > baseline is good)
     * </p>
     *
     * @param baseline Baseline algorithm metrics
     */
    public void calculateImprovementOverBaseline(AlgorithmMetrics baseline) {
        if (isBaselineAlgorithm) {
            // Baseline has 0% improvement over itself
            percentageImprovement.put("migrations", 0.0);
            percentageImprovement.put("overloads", 0.0);
            percentageImprovement.put("utilization", 0.0);
            percentageImprovement.put("sla_violations", 0.0);
            percentageImprovement.put("decision_time", 0.0);
            return;
        }

        // For minimization objectives: improvement = (baseline - candidate) / baseline * 100
        // If candidate < baseline, improvement is positive (good)
        percentageImprovement.put("migrations",
            calculateMinimizationImprovement(baseline.getTotalMigrations(), aggregatedMetrics.getTotalMigrations()));
        percentageImprovement.put("overloads",
            calculateMinimizationImprovement(baseline.getOverloadEvents(), aggregatedMetrics.getOverloadEvents()));
        percentageImprovement.put("sla_violations",
            calculateMinimizationImprovement(baseline.getSlaViolations(), aggregatedMetrics.getSlaViolations()));
        percentageImprovement.put("decision_time",
            calculateMinimizationImprovement(baseline.getDecisionTimeMs(), aggregatedMetrics.getDecisionTimeMs()));

        // For maximization objectives: improvement = (candidate - baseline) / baseline * 100
        // If candidate > baseline, improvement is positive (good)
        percentageImprovement.put("utilization",
            calculateMaximizationImprovement(baseline.getAverageUtilization(), aggregatedMetrics.getAverageUtilization()));
    }

    /**
     * Calculates improvement percentage for minimization objectives.
     *
     * @param baseline Baseline value
     * @param candidate Candidate value
     * @return Percentage improvement (positive = better)
     */
    private double calculateMinimizationImprovement(double baseline, double candidate) {
        if (baseline == 0.0) return 0.0; // Avoid division by zero
        return ((baseline - candidate) / baseline) * 100.0;
    }

    /**
     * Calculates improvement percentage for maximization objectives.
     *
     * @param baseline Baseline value
     * @param candidate Candidate value
     * @return Percentage improvement (positive = better)
     */
    private double calculateMaximizationImprovement(double baseline, double candidate) {
        if (baseline == 0.0) return 0.0; // Avoid division by zero
        return ((candidate - baseline) / baseline) * 100.0;
    }

    /**
     * Gets the algorithm name.
     *
     * @return Algorithm identifier
     */
    public String getAlgorithmName() {
        return algorithmName;
    }

    /**
     * Gets the aggregated metrics.
     *
     * @return AlgorithmMetrics object
     */
    public AlgorithmMetrics getAggregatedMetrics() {
        return aggregatedMetrics;
    }

    /**
     * Gets the percentage improvement map.
     *
     * @return Map of metric names to improvement percentages
     */
    public Map<String, Double> getPercentageImprovement() {
        return percentageImprovement;
    }

    /**
     * Checks if this is the baseline algorithm.
     *
     * @return true if baseline, false otherwise
     */
    public boolean isBaselineAlgorithm() {
        return isBaselineAlgorithm;
    }

    /**
     * Checks if this algorithm is Pareto-optimal.
     *
     * @return true if Pareto-optimal, false otherwise
     */
    public boolean isParetoOptimal() {
        return isParetoOptimal;
    }
}
