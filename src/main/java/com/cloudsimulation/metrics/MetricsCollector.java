package com.cloudsimulation.metrics;

import com.cloudsimulation.core.CloudSimIntegration;

/**
 * Interface for metrics collection implementations.
 * Allows multiple metrics collection strategies (hierarchical, sampling-based, etc.).
 */
public interface MetricsCollector {
    /**
     * Collects current system metrics from the simulation.
     *
     * @param cloudSim CloudSim integration instance to collect metrics from
     * @return MetricsSnapshot containing current system state
     */
    MetricsSnapshot collect(CloudSimIntegration cloudSim);

    /**
     * Gets the total number of VM migrations that have occurred.
     *
     * @return Cumulative migration count
     */
    int getTotalMigrations();

    /**
     * Gets the overall average CPU utilization.
     *
     * @return Average CPU utilization across all hosts
     */
    double getAverageUtilization();
}
