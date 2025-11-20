package com.cloudsimulation.metrics;

/**
 * Captures system state at a specific simulation tick for analysis.
 * Immutable data class representing a timestamped metrics snapshot.
 */
public class MetricsSnapshot {
    private final double timestamp;
    private final double avgCpuUtilization;
    private final double avgMemoryUtilization;
    private final int overloadedVmCount;
    private final int underloadedVmCount;
    private final int totalMigrations;
    private final int activeVmCount;
    private final double powerConsumption;

    /**
     * Constructs a new MetricsSnapshot with all metrics.
     *
     * @param timestamp Simulation time in seconds
     * @param avgCpuUtilization Average CPU usage across all hosts (0-1 range)
     * @param avgMemoryUtilization Average memory usage across all hosts (0-1 range)
     * @param overloadedVmCount Number of VMs exceeding 90% CPU threshold
     * @param underloadedVmCount Number of VMs below minimum threshold (20%)
     * @param totalMigrations Cumulative VM migrations to this point
     * @param activeVmCount Currently running VMs
     * @param powerConsumption Estimated watts (optional, for future use)
     */
    public MetricsSnapshot(double timestamp, double avgCpuUtilization, double avgMemoryUtilization,
                          int overloadedVmCount, int underloadedVmCount, int totalMigrations,
                          int activeVmCount, double powerConsumption) {
        this.timestamp = timestamp;
        this.avgCpuUtilization = avgCpuUtilization;
        this.avgMemoryUtilization = avgMemoryUtilization;
        this.overloadedVmCount = overloadedVmCount;
        this.underloadedVmCount = underloadedVmCount;
        this.totalMigrations = totalMigrations;
        this.activeVmCount = activeVmCount;
        this.powerConsumption = powerConsumption;
    }

    /**
     * Gets the simulation timestamp.
     *
     * @return Simulation time in seconds
     */
    public double getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the average CPU utilization across all hosts.
     *
     * @return Average CPU utilization (0-1 range)
     */
    public double getAvgCpuUtilization() {
        return avgCpuUtilization;
    }

    /**
     * Gets the average memory utilization across all hosts.
     *
     * @return Average memory utilization (0-1 range)
     */
    public double getAvgMemoryUtilization() {
        return avgMemoryUtilization;
    }

    /**
     * Gets the count of overloaded VMs.
     *
     * @return Number of VMs exceeding 90% CPU threshold
     */
    public int getOverloadedVmCount() {
        return overloadedVmCount;
    }

    /**
     * Gets the count of underloaded VMs.
     *
     * @return Number of VMs below minimum threshold (20%)
     */
    public int getUnderloadedVmCount() {
        return underloadedVmCount;
    }

    /**
     * Gets the total number of migrations.
     *
     * @return Cumulative VM migrations to this point
     */
    public int getTotalMigrations() {
        return totalMigrations;
    }

    /**
     * Gets the count of active VMs.
     *
     * @return Currently running VMs
     */
    public int getActiveVmCount() {
        return activeVmCount;
    }

    /**
     * Gets the estimated power consumption.
     *
     * @return Estimated watts (optional, for future use)
     */
    public double getPowerConsumption() {
        return powerConsumption;
    }

    @Override
    public String toString() {
        return String.format(
            "MetricsSnapshot{timestamp=%.2f, avgCpu=%.2f, avgMem=%.2f, overloaded=%d, underloaded=%d, migrations=%d, activeVms=%d, power=%.2f}",
            timestamp, avgCpuUtilization, avgMemoryUtilization, overloadedVmCount,
            underloadedVmCount, totalMigrations, activeVmCount, powerConsumption
        );
    }
}
