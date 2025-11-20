package com.cloudsimulation.workload;

import com.cloudsimulation.models.CloudletDescriptor;
import com.cloudsimulation.models.WorkloadConfig;
import com.cloudsimulation.utils.RandomSeed;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * WorkloadGenerator creates reproducible workload patterns for simulation testing.
 * Uses seeded RNG to ensure deterministic cloudlet generation.
 *
 * Supports 5 pattern types: steady, burst, gradual_increase, oscillating, diurnal.
 * All workload generation uses RandomSeed.getRandom() for reproducibility.
 */
public class WorkloadGenerator {
    private static final Logger logger = LoggerFactory.getLogger(WorkloadGenerator.class);

    /**
     * Generates cloudlets based on workload configuration with reproducible patterns.
     * Initializes RandomSeed for deterministic behavior and delegates to PatternLibrary.
     *
     * @param config Workload configuration specifying pattern and parameters
     * @return List of cloudlets for the simulation
     * @throws IllegalArgumentException if pattern type is unknown
     */
    public List<Cloudlet> generateCloudlets(WorkloadConfig config) {
        // Validate configuration
        config.validate();

        // Initialize random seed for reproducibility
        RandomSeed.setSeed(config.getSeed());
        logger.info("Generating workload with pattern: {}, seed: {}, duration: {}s",
            config.getPatternType(), config.getSeed(), config.getDuration());

        // Generate pattern-specific descriptors
        List<CloudletDescriptor> descriptors = generatePattern(config);

        // Convert descriptors to cloudlets
        List<Cloudlet> cloudlets = new ArrayList<>(descriptors.size());
        for (int i = 0; i < descriptors.size(); i++) {
            CloudletDescriptor desc = descriptors.get(i);
            Cloudlet cloudlet = CloudletFactory.createCloudlet(
                i,
                desc.getLength(),
                desc.getFileSize(),
                desc.getOutputSize()
            );
            // Note: CloudSim Plus doesn't directly support arrival time on Cloudlet
            // This would be handled by submission scheduler in SimulationRunner
            cloudlets.add(cloudlet);
        }

        logger.info("Workload generation complete: {} cloudlets created", cloudlets.size());
        return cloudlets;
    }

    /**
     * Delegates to PatternLibrary based on pattern type.
     *
     * @param config Workload configuration
     * @return List of cloudlet descriptors
     * @throws IllegalArgumentException if pattern type is unknown
     */
    private List<CloudletDescriptor> generatePattern(WorkloadConfig config) {
        return switch (config.getPatternType().toLowerCase()) {
            case "steady" -> PatternLibrary.generateSteadyPattern(config);
            case "burst" -> PatternLibrary.generateBurstPattern(config);
            case "gradual_increase" -> PatternLibrary.generateGradualIncreasePattern(config);
            case "oscillating" -> PatternLibrary.generateOscillatingPattern(config);
            case "diurnal" -> PatternLibrary.generateDiurnalPattern(config);
            default -> throw new IllegalArgumentException("Unknown pattern type: " + config.getPatternType());
        };
    }

    /**
     * Generates a steady workload with consistent cloudlets per tick.
     * Maintained for backward compatibility.
     *
     * @param cloudletsPerTick Number of cloudlets to generate per simulation tick
     * @param duration Total simulation duration in ticks
     * @return List of cloudlets for the entire simulation
     */
    public List<Cloudlet> generateSteadyWorkload(int cloudletsPerTick, int duration) {
        WorkloadConfig config = new WorkloadConfig("steady", System.currentTimeMillis(), duration, cloudletsPerTick);
        return generateCloudlets(config);
    }

    /**
     * Gets the current cloudlet count for monitoring.
     *
     * @return Current number of cloudlets in the workload
     */
    public int getCurrentLoad() {
        return 0; // Placeholder for future implementation
    }
}
