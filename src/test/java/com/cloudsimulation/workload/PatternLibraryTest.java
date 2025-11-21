package com.cloudsimulation.workload;

import com.cloudsimulation.models.CloudletDescriptor;
import com.cloudsimulation.models.WorkloadConfig;
import com.cloudsimulation.utils.RandomSeed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PatternLibrary workload pattern generators.
 */
public class PatternLibraryTest {
    private WorkloadConfig steadyConfig;
    private WorkloadConfig burstConfig;
    private WorkloadConfig gradualConfig;
    private WorkloadConfig oscillatingConfig;
    private WorkloadConfig diurnalConfig;

    @BeforeEach
    public void setup() {
        steadyConfig = new WorkloadConfig("steady", 42L, 100, 5.0);

        burstConfig = new WorkloadConfig("burst", 42L, 100, 5.0);
        burstConfig.setBurstMultiplier(10.0);

        gradualConfig = new WorkloadConfig("gradual_increase", 42L, 100, 5.0);
        gradualConfig.setRampUpDuration(50);

        oscillatingConfig = new WorkloadConfig("oscillating", 42L, 100, 5.0);
        oscillatingConfig.setFrequency(2.0);

        diurnalConfig = new WorkloadConfig("diurnal", 42L, 86400, 5.0);  // 24 hours
    }

    @Test
    public void testSteadyPatternConstantRate() {
        RandomSeed.setSeed(steadyConfig.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateSteadyPattern(steadyConfig);

        // Verify total count
        int expectedCount = (int) (steadyConfig.getIntensity() * steadyConfig.getDuration());
        assertEquals(expectedCount, descriptors.size(), "Cloudlet count should match intensity * duration");

        // Verify constant arrival rate (check inter-arrival times)
        double expectedInterval = 1.0 / steadyConfig.getIntensity();
        for (int i = 1; i < Math.min(10, descriptors.size()); i++) {
            double interval = descriptors.get(i).getArrivalTime() - descriptors.get(i - 1).getArrivalTime();
            assertEquals(expectedInterval, interval, 0.01, "Inter-arrival time should be constant");
        }
    }

    @Test
    public void testBurstPatternHasSpike() {
        RandomSeed.setSeed(burstConfig.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateBurstPattern(burstConfig);

        // Count cloudlets in spike window [midpoint - 10, midpoint + 10]
        double midpoint = burstConfig.getDuration() / 2.0;
        long spikeCount = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= midpoint - 10 && d.getArrivalTime() <= midpoint + 10)
            .count();

        // Count cloudlets in normal window [0, 10]
        long normalCount = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 0 && d.getArrivalTime() <= 10)
            .count();

        assertTrue(spikeCount > normalCount * 5, "Spike window should have significantly more cloudlets");
    }

    @Test
    public void testGradualIncreasePatternShowsLinearGrowth() {
        RandomSeed.setSeed(gradualConfig.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateGradualIncreasePattern(gradualConfig);

        // Count cloudlets in early window [0, 10]
        long earlyCount = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 0 && d.getArrivalTime() <= 10)
            .count();

        // Count cloudlets in late window [90, 100]
        long lateCount = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 90 && d.getArrivalTime() <= 100)
            .count();

        assertTrue(lateCount > earlyCount, "Late window should have more cloudlets due to ramp-up");
    }

    @Test
    public void testOscillatingPatternFollowsSineWave() {
        RandomSeed.setSeed(oscillatingConfig.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateOscillatingPattern(oscillatingConfig);

        // Verify cloudlet count is reasonable
        assertTrue(descriptors.size() > 100, "Should generate reasonable number of cloudlets");

        // Verify oscillation exists by checking variance in arrival rates
        // Sample arrival rates at different time windows
        long window1 = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 0 && d.getArrivalTime() <= 5)
            .count();

        long window2 = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 25 && d.getArrivalTime() <= 30)
            .count();

        // With 2 cycles in 100 seconds, arrival rates should vary
        assertNotEquals(window1, window2, "Arrival rates should vary across time windows");
    }

    @Test
    public void testDiurnalPatternHas24HourPeriodicity() {
        RandomSeed.setSeed(diurnalConfig.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateDiurnalPattern(diurnalConfig);

        // Count cloudlets in "business hours" peak window (around 12 hours / 43200 seconds)
        long peakCount = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 40000 && d.getArrivalTime() <= 45000)
            .count();

        // Count cloudlets in "night hours" low window (around 0 hours / 0 seconds)
        long lowCount = descriptors.stream()
            .filter(d -> d.getArrivalTime() >= 0 && d.getArrivalTime() <= 5000)
            .count();

        assertTrue(peakCount > lowCount, "Peak hours should have more cloudlets than night hours");
    }

    @Test
    public void testReproducibilityWithSameSeed() {
        WorkloadConfig config1 = new WorkloadConfig("steady", 123L, 50, 3.0);
        WorkloadConfig config2 = new WorkloadConfig("steady", 123L, 50, 3.0);

        RandomSeed.setSeed(123L);
        List<CloudletDescriptor> descriptors1 = PatternLibrary.generateSteadyPattern(config1);

        RandomSeed.setSeed(123L);
        List<CloudletDescriptor> descriptors2 = PatternLibrary.generateSteadyPattern(config2);

        assertEquals(descriptors1.size(), descriptors2.size(), "Same seed should produce same count");
        for (int i = 0; i < descriptors1.size(); i++) {
            assertEquals(descriptors1.get(i), descriptors2.get(i),
                "Cloudlet descriptor " + i + " should be identical");
        }
    }

    @Test
    public void testDifferentSeedsProduceDifferentResults() {
        WorkloadConfig config1 = new WorkloadConfig("steady", 111L, 50, 3.0);
        WorkloadConfig config2 = new WorkloadConfig("steady", 222L, 50, 3.0);

        RandomSeed.setSeed(111L);
        List<CloudletDescriptor> descriptors1 = PatternLibrary.generateSteadyPattern(config1);

        RandomSeed.setSeed(222L);
        List<CloudletDescriptor> descriptors2 = PatternLibrary.generateSteadyPattern(config2);

        assertEquals(descriptors1.size(), descriptors2.size(), "Same params should produce same count");

        // File sizes should differ due to different seeds
        boolean foundDifference = false;
        for (int i = 0; i < Math.min(descriptors1.size(), descriptors2.size()); i++) {
            if (!descriptors1.get(i).equals(descriptors2.get(i))) {
                foundDifference = true;
                break;
            }
        }
        assertTrue(foundDifference, "Different seeds should produce different random values");
    }

    @Test
    public void testCloudletCountMatchesExpected() {
        RandomSeed.setSeed(steadyConfig.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateSteadyPattern(steadyConfig);

        int expected = (int) (steadyConfig.getIntensity() * steadyConfig.getDuration());
        assertEquals(expected, descriptors.size(), "Cloudlet count should match intensity * duration");
    }

    @Test
    public void testSteadyPatternHeterogeneousDistribution() {
        // Generate 1000 cloudlets for statistical significance
        WorkloadConfig config = new WorkloadConfig("steady", 42L, 100, 10.0);
        RandomSeed.setSeed(config.getSeed());
        List<CloudletDescriptor> descriptors = PatternLibrary.generateSteadyPattern(config);

        // Classify cloudlets into light/medium/heavy buckets
        int lightCount = 0, mediumCount = 0, heavyCount = 0;
        for (CloudletDescriptor d : descriptors) {
            long length = d.getLength();
            if (length >= 1000 && length < 5001) {
                lightCount++;
            } else if (length >= 10000 && length < 50001) {
                mediumCount++;
            } else if (length >= 100000 && length <= 200000) {
                heavyCount++;
            }
        }

        // Validate 70-20-10 distribution with ±5% tolerance
        double lightPct = (lightCount * 100.0) / descriptors.size();
        double mediumPct = (mediumCount * 100.0) / descriptors.size();
        double heavyPct = (heavyCount * 100.0) / descriptors.size();

        assertTrue(lightPct >= 65 && lightPct <= 75,
            String.format("Light cloudlets should be ~70%% (got %.1f%%)", lightPct));
        assertTrue(mediumPct >= 15 && mediumPct <= 25,
            String.format("Medium cloudlets should be ~20%% (got %.1f%%)", mediumPct));
        assertTrue(heavyPct >= 5 && heavyPct <= 15,
            String.format("Heavy cloudlets should be ~10%% (got %.1f%%)", heavyPct));
    }

    @Test
    public void testAllPatternsUseHeterogeneousDistribution() {
        // Test that all pattern types generate heterogeneous cloudlets
        WorkloadConfig testConfig = new WorkloadConfig("test", 42L, 100, 10.0);
        testConfig.setBurstMultiplier(5.0);
        testConfig.setRampUpDuration(50);
        testConfig.setFrequency(2.0);

        // Test each pattern type
        String[] patterns = {"steady", "burst", "gradual_increase", "oscillating", "diurnal"};

        for (String patternType : patterns) {
            RandomSeed.setSeed(42L);
            List<CloudletDescriptor> descriptors;

            switch (patternType) {
                case "steady":
                    descriptors = PatternLibrary.generateSteadyPattern(testConfig);
                    break;
                case "burst":
                    descriptors = PatternLibrary.generateBurstPattern(testConfig);
                    break;
                case "gradual_increase":
                    descriptors = PatternLibrary.generateGradualIncreasePattern(testConfig);
                    break;
                case "oscillating":
                    descriptors = PatternLibrary.generateOscillatingPattern(testConfig);
                    break;
                case "diurnal":
                    descriptors = PatternLibrary.generateDiurnalPattern(testConfig);
                    break;
                default:
                    continue;
            }

            // Verify heterogeneous distribution exists
            boolean hasLight = false, hasMedium = false, hasHeavy = false;
            for (CloudletDescriptor d : descriptors) {
                long length = d.getLength();
                if (length >= 1000 && length < 5001) hasLight = true;
                if (length >= 10000 && length < 50001) hasMedium = true;
                if (length >= 100000 && length <= 200000) hasHeavy = true;
            }

            assertTrue(hasLight, String.format("%s pattern should have light cloudlets", patternType));
            assertTrue(hasMedium, String.format("%s pattern should have medium cloudlets", patternType));
            assertTrue(hasHeavy, String.format("%s pattern should have heavy cloudlets", patternType));
        }
    }
}
