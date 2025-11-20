package com.cloudsimulation.metrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculates Pareto dominance for multi-objective algorithm comparison.
 * Identifies non-dominated solutions (Pareto front) from algorithm metrics.
 *
 * <p><b>Pareto Dominance Definition:</b></p>
 * <p>Solution A dominates Solution B if:</p>
 * <ul>
 *   <li>A is no worse than B in all objectives</li>
 *   <li>A is strictly better than B in at least one objective</li>
 * </ul>
 *
 * <p><b>Objectives for Load Balancing:</b></p>
 * <ul>
 *   <li><b>Minimize:</b> totalMigrations, overloadEvents, slaViolations (lower is better)</li>
 *   <li><b>Maximize:</b> averageUtilization (higher resource efficiency is better)</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>
 * ParetoCalculator calculator = new ParetoCalculator();
 * List&lt;AlgorithmMetrics&gt; allMetrics = List.of(threshold, nsga2, hybrid);
 * List&lt;AlgorithmMetrics&gt; paretoFront = calculator.findNonDominatedSolutions(allMetrics);
 * // paretoFront contains only non-dominated algorithms
 * </pre>
 */
public class ParetoCalculator {

    /**
     * Checks if solution A dominates solution B.
     *
     * <p>Dominance is determined across 4 objectives:
     * <ul>
     *   <li>Migrations (minimize)</li>
     *   <li>Overload events (minimize)</li>
     *   <li>SLA violations (minimize)</li>
     *   <li>Utilization (maximize)</li>
     * </ul></p>
     *
     * @param a First solution
     * @param b Second solution
     * @return true if A dominates B, false otherwise
     */
    public boolean dominates(AlgorithmMetrics a, AlgorithmMetrics b) {
        boolean betterInAtLeastOne = false;

        // Check minimization objectives (lower is better)
        // If A is worse in any objective, it cannot dominate
        if (a.getTotalMigrations() > b.getTotalMigrations()) return false;
        if (a.getTotalMigrations() < b.getTotalMigrations()) betterInAtLeastOne = true;

        if (a.getOverloadEvents() > b.getOverloadEvents()) return false;
        if (a.getOverloadEvents() < b.getOverloadEvents()) betterInAtLeastOne = true;

        if (a.getSlaViolations() > b.getSlaViolations()) return false;
        if (a.getSlaViolations() < b.getSlaViolations()) betterInAtLeastOne = true;

        // Check maximization objective (higher is better)
        if (a.getAverageUtilization() < b.getAverageUtilization()) return false;
        if (a.getAverageUtilization() > b.getAverageUtilization()) betterInAtLeastOne = true;

        // A dominates B only if it's better in at least one objective and no worse in all others
        return betterInAtLeastOne;
    }

    /**
     * Finds non-dominated solutions (Pareto front) from list of metrics.
     *
     * <p>A solution is non-dominated if no other solution in the list dominates it.
     * The Pareto front represents the set of best trade-offs across all objectives.</p>
     *
     * <p><b>Complexity:</b> O(n²) where n is the number of solutions.</p>
     *
     * @param metricsList List of algorithm metrics to analyze
     * @return List of non-dominated AlgorithmMetrics (Pareto front)
     */
    public List<AlgorithmMetrics> findNonDominatedSolutions(List<AlgorithmMetrics> metricsList) {
        List<AlgorithmMetrics> paretoFront = new ArrayList<>();

        for (AlgorithmMetrics candidate : metricsList) {
            boolean isDominated = false;

            // Check if any existing solution dominates candidate
            for (AlgorithmMetrics existing : metricsList) {
                if (candidate != existing && dominates(existing, candidate)) {
                    isDominated = true;
                    break;
                }
            }

            if (!isDominated) {
                paretoFront.add(candidate);
            }
        }

        return paretoFront;
    }
}
