package com.cloudsimulation.algorithms;

import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.LoadBalancingPlan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * No-operation policy that performs no load balancing.
 *
 * <p>Mock implementation that always returns an empty migration plan.
 * Useful for baseline comparisons, testing, and as a reference implementation.</p>
 *
 * <p><b>Behavior:</b></p>
 * <ul>
 *   <li>Always returns empty LoadBalancingPlan (no migrations)</li>
 *   <li>Logs each evaluation for debugging</li>
 *   <li>Stateless - reset() is a no-op</li>
 * </ul>
 *
 * <p><b>Use Cases:</b></p>
 * <ul>
 *   <li>Baseline performance measurement (no load balancing)</li>
 *   <li>Testing infrastructure without migration overhead</li>
 *   <li>Reference implementation for new policy developers</li>
 * </ul>
 */
public class NoOpPolicy implements LoadBalancingPolicy {
    private static final Logger logger = LoggerFactory.getLogger(NoOpPolicy.class);
    private static final String POLICY_NAME = "noop";

    private long evaluationCount = 0;

    /**
     * Constructs a new NoOpPolicy.
     */
    public NoOpPolicy() {
        logger.info("NoOpPolicy initialized");
    }

    /**
     * Evaluates system state and returns empty migration plan.
     *
     * <p>This implementation always returns an empty plan regardless of
     * system state. No migrations are ever suggested.</p>
     *
     * @param snapshot Current system metrics (used only for logging)
     * @return Empty LoadBalancingPlan with no migrations
     * @throws NullPointerException if snapshot is null
     */
    @Override
    public LoadBalancingPlan evaluate(MetricsSnapshot snapshot) {
        if (snapshot == null) {
            throw new NullPointerException("MetricsSnapshot cannot be null");
        }

        evaluationCount++;

        logger.debug(
            "NoOpPolicy.evaluate() called - evaluation #{}, timestamp={}, avgCpu={}, activeVms={}",
            evaluationCount,
            snapshot.getTimestamp(),
            String.format("%.2f", snapshot.getAvgCpuUtilization()),
            snapshot.getActiveVmCount()
        );

        // Return empty plan - no migrations
        return new LoadBalancingPlan.Builder()
            .timestamp(snapshot.getTimestamp())
            .algorithmName(POLICY_NAME)
            .computationTime(0L)
            .build();
    }

    /**
     * Returns the policy name.
     *
     * @return "noop"
     */
    @Override
    public String getName() {
        return POLICY_NAME;
    }

    /**
     * Resets policy state.
     *
     * <p>NoOpPolicy is stateless except for evaluation counter.
     * This method resets the counter.</p>
     */
    @Override
    public void reset() {
        logger.debug("NoOpPolicy.reset() called - resetting evaluation counter from {}", evaluationCount);
        evaluationCount = 0;
    }

    /**
     * Gets the number of times this policy has been evaluated.
     *
     * @return Evaluation count since creation or last reset
     */
    public long getEvaluationCount() {
        return evaluationCount;
    }
}
