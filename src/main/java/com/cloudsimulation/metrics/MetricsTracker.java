package com.cloudsimulation.metrics;

import com.cloudsimulation.models.LoadBalancingPlan;

import java.util.ArrayList;
import java.util.List;

/**
 * Tracks per-evaluation metrics for load balancing algorithms.
 * Records metrics at each evaluation cycle for time-series analysis.
 *
 * Note: This class is NOT thread-safe. Use one instance per algorithm evaluation thread.
 */
public class MetricsTracker {
    private final List<AlgorithmMetrics> perCycleMetrics;
    private MetricsSnapshot lastSnapshot;
    private int consecutiveOverloadTicks;
    private static final int SLA_VIOLATION_THRESHOLD_TICKS = 5;

    /**
     * Creates a new MetricsTracker instance.
     */
    public MetricsTracker() {
        this.perCycleMetrics = new ArrayList<>();
        this.consecutiveOverloadTicks = 0;
    }

    /**
     * Records metrics for a single algorithm evaluation.
     *
     * @param plan LoadBalancingPlan from algorithm evaluation
     * @param snapshot MetricsSnapshot of current system state
     * @param computationTime Time taken to compute decision (milliseconds)
     * @return AlgorithmMetrics for this evaluation cycle
     */
    public AlgorithmMetrics recordEvaluation(LoadBalancingPlan plan, MetricsSnapshot snapshot, long computationTime) {
        // Extract metrics from plan and snapshot
        String algorithmName = plan.getAlgorithmName();
        double timestamp = plan.getTimestamp();
        int migrations = plan.getMigrationCount();
        int overloadEvents = snapshot.getOverloadedVmCount();
        double avgUtilization = snapshot.getAvgCpuUtilization();

        // Track SLA violations (overload persisting > threshold)
        int slaViolations = 0;
        if (overloadEvents > 0) {
            consecutiveOverloadTicks++;
            if (consecutiveOverloadTicks >= SLA_VIOLATION_THRESHOLD_TICKS) {
                slaViolations = 1;
            }
        } else {
            consecutiveOverloadTicks = 0;
        }

        // Build AlgorithmMetrics instance
        AlgorithmMetrics metrics = new AlgorithmMetrics.Builder()
            .algorithmName(algorithmName)
            .timestamp(timestamp)
            .totalMigrations(migrations)
            .overloadEvents(overloadEvents)
            .averageUtilization(avgUtilization)
            .slaViolations(slaViolations)
            .decisionTimeMs(computationTime)
            .build();

        perCycleMetrics.add(metrics);
        lastSnapshot = snapshot;

        return metrics;
    }

    /**
     * Gets all per-cycle metrics recorded.
     *
     * @return List of AlgorithmMetrics for time-series analysis
     */
    public List<AlgorithmMetrics> getPerCycleMetrics() {
        return new ArrayList<>(perCycleMetrics);
    }

    /**
     * Gets the most recent metrics snapshot.
     * Useful for external trend analysis and state comparison.
     *
     * @return Last MetricsSnapshot, or null if no evaluations recorded
     */
    public MetricsSnapshot getLastSnapshot() {
        return lastSnapshot;
    }
}
