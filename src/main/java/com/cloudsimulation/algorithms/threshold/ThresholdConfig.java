package com.cloudsimulation.algorithms.threshold;

/**
 * Configuration for threshold-based load balancing algorithm.
 * Defines CPU and memory thresholds for triggering migrations.
 *
 * <p><b>Threshold Semantics:</b></p>
 * <ul>
 *   <li><b>Upper Threshold:</b> Triggers migration when host exceeds this utilization</li>
 *   <li><b>Lower Threshold:</b> Safety limit - target host must remain below this after migration</li>
 * </ul>
 *
 * <p><b>Default Configuration:</b></p>
 * <ul>
 *   <li>CPU Upper: 0.8 (80%) - migrate when host CPU > 80%</li>
 *   <li>CPU Lower: 0.7 (70%) - target must stay below 70%</li>
 *   <li>Memory Upper: 0.8 (80%) - migrate when host memory > 80%</li>
 *   <li>Memory Lower: 0.7 (70%) - target must stay below 70%</li>
 * </ul>
 */
public class ThresholdConfig {
    private static final double DEFAULT_CPU_UPPER_THRESHOLD = 0.8;
    private static final double DEFAULT_CPU_LOWER_THRESHOLD = 0.7;
    private static final double DEFAULT_MEMORY_UPPER_THRESHOLD = 0.8;
    private static final double DEFAULT_MEMORY_LOWER_THRESHOLD = 0.7;

    private double cpuUpperThreshold;
    private double cpuLowerThreshold;
    private double memoryUpperThreshold;
    private double memoryLowerThreshold;

    /**
     * Creates a new ThresholdConfig with default thresholds.
     */
    public ThresholdConfig() {
        this.cpuUpperThreshold = DEFAULT_CPU_UPPER_THRESHOLD;
        this.cpuLowerThreshold = DEFAULT_CPU_LOWER_THRESHOLD;
        this.memoryUpperThreshold = DEFAULT_MEMORY_UPPER_THRESHOLD;
        this.memoryLowerThreshold = DEFAULT_MEMORY_LOWER_THRESHOLD;
    }

    /**
     * Creates a new ThresholdConfig with custom thresholds.
     *
     * @param cpuUpperThreshold CPU upper threshold (0-1 range)
     * @param cpuLowerThreshold CPU lower threshold (0-1 range)
     * @param memoryUpperThreshold Memory upper threshold (0-1 range)
     * @param memoryLowerThreshold Memory lower threshold (0-1 range)
     * @throws IllegalArgumentException if thresholds are invalid
     */
    public ThresholdConfig(double cpuUpperThreshold, double cpuLowerThreshold,
                          double memoryUpperThreshold, double memoryLowerThreshold) {
        setCpuUpperThreshold(cpuUpperThreshold);
        setCpuLowerThreshold(cpuLowerThreshold);
        setMemoryUpperThreshold(memoryUpperThreshold);
        setMemoryLowerThreshold(memoryLowerThreshold);
        validate();
    }

    /**
     * Gets the CPU upper threshold.
     *
     * @return CPU upper threshold (0-1 range)
     */
    public double getCpuUpperThreshold() {
        return cpuUpperThreshold;
    }

    /**
     * Sets the CPU upper threshold.
     *
     * @param cpuUpperThreshold CPU upper threshold (0-1 range)
     * @throws IllegalArgumentException if threshold is not in valid range
     */
    public void setCpuUpperThreshold(double cpuUpperThreshold) {
        if (cpuUpperThreshold < 0.0 || cpuUpperThreshold > 1.0) {
            throw new IllegalArgumentException(
                "CPU upper threshold must be between 0.0 and 1.0, got: " + cpuUpperThreshold
            );
        }
        this.cpuUpperThreshold = cpuUpperThreshold;
    }

    /**
     * Gets the CPU lower threshold.
     *
     * @return CPU lower threshold (0-1 range)
     */
    public double getCpuLowerThreshold() {
        return cpuLowerThreshold;
    }

    /**
     * Sets the CPU lower threshold.
     *
     * @param cpuLowerThreshold CPU lower threshold (0-1 range)
     * @throws IllegalArgumentException if threshold is not in valid range
     */
    public void setCpuLowerThreshold(double cpuLowerThreshold) {
        if (cpuLowerThreshold < 0.0 || cpuLowerThreshold > 1.0) {
            throw new IllegalArgumentException(
                "CPU lower threshold must be between 0.0 and 1.0, got: " + cpuLowerThreshold
            );
        }
        this.cpuLowerThreshold = cpuLowerThreshold;
    }

    /**
     * Gets the memory upper threshold.
     *
     * @return Memory upper threshold (0-1 range)
     */
    public double getMemoryUpperThreshold() {
        return memoryUpperThreshold;
    }

    /**
     * Sets the memory upper threshold.
     *
     * @param memoryUpperThreshold Memory upper threshold (0-1 range)
     * @throws IllegalArgumentException if threshold is not in valid range
     */
    public void setMemoryUpperThreshold(double memoryUpperThreshold) {
        if (memoryUpperThreshold < 0.0 || memoryUpperThreshold > 1.0) {
            throw new IllegalArgumentException(
                "Memory upper threshold must be between 0.0 and 1.0, got: " + memoryUpperThreshold
            );
        }
        this.memoryUpperThreshold = memoryUpperThreshold;
    }

    /**
     * Gets the memory lower threshold.
     *
     * @return Memory lower threshold (0-1 range)
     */
    public double getMemoryLowerThreshold() {
        return memoryLowerThreshold;
    }

    /**
     * Sets the memory lower threshold.
     *
     * @param memoryLowerThreshold Memory lower threshold (0-1 range)
     * @throws IllegalArgumentException if threshold is not in valid range
     */
    public void setMemoryLowerThreshold(double memoryLowerThreshold) {
        if (memoryLowerThreshold < 0.0 || memoryLowerThreshold > 1.0) {
            throw new IllegalArgumentException(
                "Memory lower threshold must be between 0.0 and 1.0, got: " + memoryLowerThreshold
            );
        }
        this.memoryLowerThreshold = memoryLowerThreshold;
    }

    /**
     * Validates that upper thresholds are greater than lower thresholds.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (cpuUpperThreshold <= cpuLowerThreshold) {
            throw new IllegalArgumentException(
                String.format(
                    "CPU upper threshold (%.2f) must be greater than lower threshold (%.2f)",
                    cpuUpperThreshold, cpuLowerThreshold
                )
            );
        }
        if (memoryUpperThreshold <= memoryLowerThreshold) {
            throw new IllegalArgumentException(
                String.format(
                    "Memory upper threshold (%.2f) must be greater than lower threshold (%.2f)",
                    memoryUpperThreshold, memoryLowerThreshold
                )
            );
        }
    }

    @Override
    public String toString() {
        return String.format(
            "ThresholdConfig{cpuUpper=%.2f, cpuLower=%.2f, memoryUpper=%.2f, memoryLower=%.2f}",
            cpuUpperThreshold, cpuLowerThreshold, memoryUpperThreshold, memoryLowerThreshold
        );
    }
}
