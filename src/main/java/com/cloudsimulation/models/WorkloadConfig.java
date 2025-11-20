package com.cloudsimulation.models;

/**
 * Configuration for workload generation with reproducible patterns.
 * Supports 5 pattern types: steady, burst, gradual_increase, oscillating, diurnal.
 */
public class WorkloadConfig {
    private String patternType;          // Pattern: "steady", "burst", "gradual_increase", "oscillating", "diurnal"
    private long seed;                   // Random seed for reproducibility
    private int duration;                // Simulation duration in seconds
    private double intensity;            // Base cloudlet arrival rate (cloudlets/second)
    private double frequency;            // For oscillating/diurnal patterns (cycles/duration)
    private double burstMultiplier;      // For burst pattern (spike intensity multiplier)
    private int rampUpDuration;          // For gradual_increase pattern (seconds to reach max)

    // Constructors
    public WorkloadConfig() {
        // Default constructor for YAML deserialization
    }

    public WorkloadConfig(String patternType, long seed, int duration, double intensity) {
        this.patternType = patternType;
        this.seed = seed;
        this.duration = duration;
        this.intensity = intensity;
        this.frequency = 1.0;            // Default: 1 cycle
        this.burstMultiplier = 5.0;      // Default: 5x spike
        this.rampUpDuration = duration / 2;  // Default: ramp over half duration
    }

    // Getters and Setters
    public String getPatternType() { return patternType; }
    public void setPatternType(String patternType) { this.patternType = patternType; }

    public long getSeed() { return seed; }
    public void setSeed(long seed) { this.seed = seed; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public double getIntensity() { return intensity; }
    public void setIntensity(double intensity) { this.intensity = intensity; }

    public double getFrequency() { return frequency; }
    public void setFrequency(double frequency) { this.frequency = frequency; }

    public double getBurstMultiplier() { return burstMultiplier; }
    public void setBurstMultiplier(double burstMultiplier) { this.burstMultiplier = burstMultiplier; }

    public int getRampUpDuration() { return rampUpDuration; }
    public void setRampUpDuration(int rampUpDuration) { this.rampUpDuration = rampUpDuration; }

    /**
     * Validates configuration parameters.
     *
     * @throws IllegalArgumentException if parameters are invalid
     */
    public void validate() {
        if (patternType == null || patternType.isEmpty()) {
            throw new IllegalArgumentException("Pattern type must be specified");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }
        if (intensity <= 0) {
            throw new IllegalArgumentException("Intensity must be positive");
        }

        // Pattern-specific validation
        String pattern = patternType.toLowerCase();

        // Validate frequency for patterns that use it (oscillating, diurnal)
        if (("oscillating".equals(pattern) || "diurnal".equals(pattern)) && frequency <= 0) {
            throw new IllegalArgumentException("Frequency must be positive for " + pattern + " pattern");
        }

        // Validate burstMultiplier for burst pattern
        if ("burst".equals(pattern) && burstMultiplier < 1.0) {
            throw new IllegalArgumentException("Burst multiplier must be >= 1.0 for burst pattern");
        }

        // Validate rampUpDuration for gradual_increase pattern
        if ("gradual_increase".equals(pattern) && (rampUpDuration <= 0 || rampUpDuration > duration)) {
            throw new IllegalArgumentException("Ramp-up duration must be positive and <= total duration for gradual_increase pattern");
        }
    }
}
