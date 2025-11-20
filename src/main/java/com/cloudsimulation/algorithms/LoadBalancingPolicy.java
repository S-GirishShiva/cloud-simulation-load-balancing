package com.cloudsimulation.algorithms;

import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.LoadBalancingPlan;

/**
 * Interface for all load balancing policy implementations.
 *
 * <p>Supports both reactive policies (threshold-based, responding to current state)
 * and proactive policies (predictive, anticipating future state).</p>
 *
 * <p><b>Implementation Guidelines:</b></p>
 * <ul>
 *   <li>Implementations must be stateless or manage their own state carefully</li>
 *   <li>Policies may be called concurrently during simulation</li>
 *   <li>Policies may be switched at runtime via PolicyManager</li>
 *   <li>The evaluate() method should return quickly (< 100ms for real-time simulation)</li>
 *   <li>Empty migration lists are valid - return empty plan when no action needed</li>
 * </ul>
 *
 * <p><b>Reactive vs Proactive Policies:</b></p>
 * <ul>
 *   <li><b>Reactive:</b> Threshold-based policies that respond to current metrics
 *       (e.g., migrate VMs when CPU > 90%)</li>
 *   <li><b>Proactive:</b> Predictive policies that anticipate future state
 *       (e.g., predict workload spike and migrate preemptively)</li>
 * </ul>
 *
 * @see MetricsSnapshot
 * @see LoadBalancingPlan
 */
public interface LoadBalancingPolicy {
    /**
     * Evaluates current system state and returns load balancing decisions.
     *
     * <p>This method is called at each simulation tick (every 5 ticks by default)
     * to determine if VM migrations are needed. Implementations should analyze
     * the provided metrics and return a plan containing zero or more migrations.</p>
     *
     * @param snapshot Current system metrics captured at this simulation tick
     * @return LoadBalancingPlan containing migration decisions (empty list if no action needed)
     * @throws NullPointerException if snapshot is null
     */
    LoadBalancingPlan evaluate(MetricsSnapshot snapshot);

    /**
     * Returns the unique name of this policy.
     *
     * <p>Policy names are used for identification in logs, configuration files,
     * and the PolicyManager. Names should be lowercase, descriptive, and unique.</p>
     *
     * @return Policy identifier (e.g., "noop", "threshold", "nsga2", "hybrid")
     */
    String getName();

    /**
     * Resets any internal state of the policy.
     *
     * <p>Called when simulation restarts or policy needs to be reinitialized.
     * Stateless policies can implement this as a no-op. Stateful policies
     * should clear any accumulated history or cached data.</p>
     *
     * <p><b>Examples of state to reset:</b></p>
     * <ul>
     *   <li>Historical metrics buffers</li>
     *   <li>Prediction model state</li>
     *   <li>Counters or accumulators</li>
     *   <li>Cached decisions</li>
     * </ul>
     */
    void reset();
}
