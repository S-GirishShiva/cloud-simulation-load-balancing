package com.cloudsimulation.workload;

import com.cloudsimulation.models.CloudletDescriptor;
import com.cloudsimulation.models.WorkloadConfig;
import com.cloudsimulation.utils.RandomSeed;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PatternLibrary provides workload pattern generators for reproducible testing.
 * All patterns use seeded RNG for deterministic behavior.
 */
public class PatternLibrary {

    /**
     * Generates steady workload pattern with constant arrival rate.
     * Formula: Arrival time t_i = i / intensity
     * Uses heterogeneous cloudlet distribution: 70% Light (1-5K MI), 20% Medium (10-50K MI), 10% Heavy (100-200K MI)
     *
     * @param config Workload configuration
     * @return List of cloudlet descriptors
     */
    public static List<CloudletDescriptor> generateSteadyPattern(WorkloadConfig config) {
        Random random = RandomSeed.getRandom();
        List<CloudletDescriptor> descriptors = new ArrayList<>();

        int totalCloudlets = (int) (config.getIntensity() * config.getDuration());
        double interArrivalTime = 1.0 / config.getIntensity();

        for (int i = 0; i < totalCloudlets; i++) {
            double arrivalTime = i * interArrivalTime;

            // Heterogeneous cloudlet distribution (70-20-10)
            double typeSelector = random.nextDouble();
            long length;
            if (typeSelector < 0.70) {
                // 70% Light cloudlets: 1,000-5,000 MI (1-2 sec on 3000 MIPS VM)
                length = 1000 + random.nextInt(4000);
            } else if (typeSelector < 0.90) {
                // 20% Medium cloudlets: 10,000-50,000 MI (3-17 sec)
                length = 10000 + random.nextInt(40000);
            } else {
                // 10% Heavy cloudlets: 100,000-200,000 MI (33-67 sec)
                length = 100000 + random.nextInt(100000);
            }

            long fileSize = 300 + random.nextInt(100);   // 300-400 MB
            long outputSize = 300 + random.nextInt(100); // 300-400 MB

            descriptors.add(new CloudletDescriptor(arrivalTime, length, fileSize, outputSize));
        }

        return descriptors;
    }

    /**
     * Generates burst pattern with sudden traffic spike at midpoint.
     * Formula: intensity * burstMultiplier during spike window [duration/2 - 10, duration/2 + 10]
     * Uses heterogeneous cloudlet distribution: 70% Light (1-5K MI), 20% Medium (10-50K MI), 10% Heavy (100-200K MI)
     *
     * @param config Workload configuration (requires burstMultiplier)
     * @return List of cloudlet descriptors
     */
    public static List<CloudletDescriptor> generateBurstPattern(WorkloadConfig config) {
        Random random = RandomSeed.getRandom();
        List<CloudletDescriptor> descriptors = new ArrayList<>();

        double midpoint = config.getDuration() / 2.0;
        double burstStart = midpoint - 10.0;
        double burstEnd = midpoint + 10.0;

        double currentTime = 0.0;

        while (currentTime < config.getDuration()) {
            // Determine current arrival rate
            double arrivalRate = config.getIntensity();
            if (currentTime >= burstStart && currentTime <= burstEnd) {
                arrivalRate *= config.getBurstMultiplier();
            }

            // Generate cloudlets for current second
            int cloudletsThisSecond = (int) arrivalRate + (random.nextDouble() < (arrivalRate % 1) ? 1 : 0);
            for (int i = 0; i < cloudletsThisSecond; i++) {
                double arrivalTime = currentTime + random.nextDouble();

                // Heterogeneous cloudlet distribution (70-20-10)
                double typeSelector = random.nextDouble();
                long length;
                if (typeSelector < 0.70) {
                    // 70% Light cloudlets: 1,000-5,000 MI (1-2 sec on 3000 MIPS VM)
                    length = 1000 + random.nextInt(4000);
                } else if (typeSelector < 0.90) {
                    // 20% Medium cloudlets: 10,000-50,000 MI (3-17 sec)
                    length = 10000 + random.nextInt(40000);
                } else {
                    // 10% Heavy cloudlets: 100,000-200,000 MI (33-67 sec)
                    length = 100000 + random.nextInt(100000);
                }

                long fileSize = 300 + random.nextInt(100);
                long outputSize = 300 + random.nextInt(100);

                descriptors.add(new CloudletDescriptor(arrivalTime, length, fileSize, outputSize));
            }

            currentTime += 1.0;
        }

        return descriptors;
    }

    /**
     * Generates gradual increase pattern with linear ramp-up.
     * Formula: intensity * (1 + t / rampUpDuration), reaches 2x intensity at rampUpDuration
     * Uses heterogeneous cloudlet distribution: 70% Light (1-5K MI), 20% Medium (10-50K MI), 10% Heavy (100-200K MI)
     *
     * @param config Workload configuration (requires rampUpDuration)
     * @return List of cloudlet descriptors
     */
    public static List<CloudletDescriptor> generateGradualIncreasePattern(WorkloadConfig config) {
        Random random = RandomSeed.getRandom();
        List<CloudletDescriptor> descriptors = new ArrayList<>();

        double currentTime = 0.0;

        while (currentTime < config.getDuration()) {
            // Calculate current arrival rate (linear ramp)
            double rampFactor = Math.min(currentTime / config.getRampUpDuration(), 1.0);
            double arrivalRate = config.getIntensity() * (1.0 + rampFactor);

            int cloudletsThisSecond = (int) arrivalRate + (random.nextDouble() < (arrivalRate % 1) ? 1 : 0);
            for (int i = 0; i < cloudletsThisSecond; i++) {
                double arrivalTime = currentTime + random.nextDouble();

                // Heterogeneous cloudlet distribution (70-20-10)
                double typeSelector = random.nextDouble();
                long length;
                if (typeSelector < 0.70) {
                    // 70% Light cloudlets: 1,000-5,000 MI (1-2 sec on 3000 MIPS VM)
                    length = 1000 + random.nextInt(4000);
                } else if (typeSelector < 0.90) {
                    // 20% Medium cloudlets: 10,000-50,000 MI (3-17 sec)
                    length = 10000 + random.nextInt(40000);
                } else {
                    // 10% Heavy cloudlets: 100,000-200,000 MI (33-67 sec)
                    length = 100000 + random.nextInt(100000);
                }

                long fileSize = 300 + random.nextInt(100);
                long outputSize = 300 + random.nextInt(100);

                descriptors.add(new CloudletDescriptor(arrivalTime, length, fileSize, outputSize));
            }

            currentTime += 1.0;
        }

        return descriptors;
    }

    /**
     * Generates oscillating pattern with sine wave variation.
     * Formula: intensity * (1 + 0.5 * sin(2π * frequency * t / duration))
     * Varies between 0.5x and 1.5x intensity
     * Uses heterogeneous cloudlet distribution: 70% Light (1-5K MI), 20% Medium (10-50K MI), 10% Heavy (100-200K MI)
     *
     * @param config Workload configuration (requires frequency)
     * @return List of cloudlet descriptors
     */
    public static List<CloudletDescriptor> generateOscillatingPattern(WorkloadConfig config) {
        Random random = RandomSeed.getRandom();
        List<CloudletDescriptor> descriptors = new ArrayList<>();

        double currentTime = 0.0;

        while (currentTime < config.getDuration()) {
            // Calculate oscillating arrival rate
            double phase = 2 * Math.PI * config.getFrequency() * currentTime / config.getDuration();
            double arrivalRate = config.getIntensity() * (1.0 + 0.5 * Math.sin(phase));

            int cloudletsThisSecond = (int) arrivalRate + (random.nextDouble() < (arrivalRate % 1) ? 1 : 0);
            for (int i = 0; i < cloudletsThisSecond; i++) {
                double arrivalTime = currentTime + random.nextDouble();

                // Heterogeneous cloudlet distribution (70-20-10)
                double typeSelector = random.nextDouble();
                long length;
                if (typeSelector < 0.70) {
                    // 70% Light cloudlets: 1,000-5,000 MI (1-2 sec on 3000 MIPS VM)
                    length = 1000 + random.nextInt(4000);
                } else if (typeSelector < 0.90) {
                    // 20% Medium cloudlets: 10,000-50,000 MI (3-17 sec)
                    length = 10000 + random.nextInt(40000);
                } else {
                    // 10% Heavy cloudlets: 100,000-200,000 MI (33-67 sec)
                    length = 100000 + random.nextInt(100000);
                }

                long fileSize = 300 + random.nextInt(100);
                long outputSize = 300 + random.nextInt(100);

                descriptors.add(new CloudletDescriptor(arrivalTime, length, fileSize, outputSize));
            }

            currentTime += 1.0;
        }

        return descriptors;
    }

    /**
     * Generates diurnal pattern simulating 24-hour business cycle.
     * Formula: intensity * max(0.2, 1 + 0.8 * sin(2π * (t - 6) / 24))
     * Simulates business hours peak (9am-5pm high load), night hours low load (10pm-6am)
     * Uses heterogeneous cloudlet distribution: 70% Light (1-5K MI), 20% Medium (10-50K MI), 10% Heavy (100-200K MI)
     *
     * @param config Workload configuration
     * @return List of cloudlet descriptors
     */
    public static List<CloudletDescriptor> generateDiurnalPattern(WorkloadConfig config) {
        Random random = RandomSeed.getRandom();
        List<CloudletDescriptor> descriptors = new ArrayList<>();

        double currentTime = 0.0;

        while (currentTime < config.getDuration()) {
            // Simulate 24-hour cycle (peak at 9am-5pm, low at night)
            double hourOfDay = (currentTime % 86400) / 3600.0;  // Convert to hour (0-24)
            double peakFactor = 1.0 + 0.8 * Math.sin(2 * Math.PI * (hourOfDay - 6.0) / 24.0);
            double arrivalRate = config.getIntensity() * Math.max(0.2, peakFactor);  // Min 20% of base

            int cloudletsThisSecond = (int) arrivalRate + (random.nextDouble() < (arrivalRate % 1) ? 1 : 0);
            for (int i = 0; i < cloudletsThisSecond; i++) {
                double arrivalTime = currentTime + random.nextDouble();

                // Heterogeneous cloudlet distribution (70-20-10)
                double typeSelector = random.nextDouble();
                long length;
                if (typeSelector < 0.70) {
                    // 70% Light cloudlets: 1,000-5,000 MI (1-2 sec on 3000 MIPS VM)
                    length = 1000 + random.nextInt(4000);
                } else if (typeSelector < 0.90) {
                    // 20% Medium cloudlets: 10,000-50,000 MI (3-17 sec)
                    length = 10000 + random.nextInt(40000);
                } else {
                    // 10% Heavy cloudlets: 100,000-200,000 MI (33-67 sec)
                    length = 100000 + random.nextInt(100000);
                }

                long fileSize = 300 + random.nextInt(100);
                long outputSize = 300 + random.nextInt(100);

                descriptors.add(new CloudletDescriptor(arrivalTime, length, fileSize, outputSize));
            }

            currentTime += 1.0;
        }

        return descriptors;
    }
}
