package com.cloudsimulation.workload;

import com.cloudsimulation.models.WorkloadConfig;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkloadGenerator.
 */
public class WorkloadGeneratorTest {
    private WorkloadGenerator generator;

    @BeforeEach
    public void setup() {
        generator = new WorkloadGenerator();
    }

    @Test
    public void testSameSeedProducesIdenticalWorkload() {
        WorkloadConfig config = new WorkloadConfig("steady", 999L, 50, 4.0);

        List<Cloudlet> cloudlets1 = generator.generateCloudlets(config);
        List<Cloudlet> cloudlets2 = generator.generateCloudlets(config);

        assertEquals(cloudlets1.size(), cloudlets2.size(), "Same seed should produce same cloudlet count");

        WorkloadValidator validator = new WorkloadValidator();
        int hash1 = validator.computeWorkloadHash(cloudlets1);
        int hash2 = validator.computeWorkloadHash(cloudlets2);

        assertEquals(hash1, hash2, "Same seed should produce identical workload hash");
    }

    @Test
    public void testDifferentSeedsProduceDifferentWorkloads() {
        WorkloadConfig config1 = new WorkloadConfig("steady", 111L, 50, 4.0);
        WorkloadConfig config2 = new WorkloadConfig("steady", 222L, 50, 4.0);

        List<Cloudlet> cloudlets1 = generator.generateCloudlets(config1);
        List<Cloudlet> cloudlets2 = generator.generateCloudlets(config2);

        assertEquals(cloudlets1.size(), cloudlets2.size(), "Same params should produce same count");

        WorkloadValidator validator = new WorkloadValidator();
        int hash1 = validator.computeWorkloadHash(cloudlets1);
        int hash2 = validator.computeWorkloadHash(cloudlets2);

        assertNotEquals(hash1, hash2, "Different seeds should produce different workload hash");
    }

    @Test
    public void testSteadyPatternIntegration() {
        WorkloadConfig config = new WorkloadConfig("steady", 42L, 100, 5.0);
        List<Cloudlet> cloudlets = generator.generateCloudlets(config);

        int expected = (int) (config.getIntensity() * config.getDuration());
        assertEquals(expected, cloudlets.size(), "Steady pattern should produce expected count");
    }

    @Test
    public void testBurstPatternIntegration() {
        WorkloadConfig config = new WorkloadConfig("burst", 42L, 100, 5.0);
        config.setBurstMultiplier(10.0);

        List<Cloudlet> cloudlets = generator.generateCloudlets(config);
        assertTrue(cloudlets.size() > 500, "Burst pattern should generate many cloudlets");
    }

    @Test
    public void testGradualIncreasePatternIntegration() {
        WorkloadConfig config = new WorkloadConfig("gradual_increase", 42L, 100, 5.0);
        config.setRampUpDuration(50);

        List<Cloudlet> cloudlets = generator.generateCloudlets(config);
        assertTrue(cloudlets.size() > 500, "Gradual increase should generate cloudlets");
    }

    @Test
    public void testOscillatingPatternIntegration() {
        WorkloadConfig config = new WorkloadConfig("oscillating", 42L, 100, 5.0);
        config.setFrequency(2.0);

        List<Cloudlet> cloudlets = generator.generateCloudlets(config);
        assertTrue(cloudlets.size() > 100, "Oscillating pattern should generate cloudlets");
    }

    @Test
    public void testDiurnalPatternIntegration() {
        WorkloadConfig config = new WorkloadConfig("diurnal", 42L, 1000, 5.0);
        List<Cloudlet> cloudlets = generator.generateCloudlets(config);

        assertTrue(cloudlets.size() > 1000, "Diurnal pattern should generate cloudlets");
    }

    @Test
    public void testInvalidPatternTypeThrowsException() {
        WorkloadConfig config = new WorkloadConfig("invalid_pattern", 42L, 50, 3.0);

        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCloudlets(config);
        }, "Invalid pattern type should throw IllegalArgumentException");
    }

    @Test
    public void testWorkloadDurationMatchesConfiguration() {
        WorkloadConfig config = new WorkloadConfig("steady", 42L, 100, 5.0);
        List<Cloudlet> cloudlets = generator.generateCloudlets(config);

        int expectedCount = (int) (config.getIntensity() * config.getDuration());
        assertEquals(expectedCount, cloudlets.size(), "Workload size should match configuration");
    }

    @Test
    public void testBackwardCompatibilityWithGenerateSteadyWorkload() {
        int cloudletsPerTick = 5;
        int duration = 100;

        List<Cloudlet> cloudlets = generator.generateSteadyWorkload(cloudletsPerTick, duration);

        int expected = cloudletsPerTick * duration;
        assertEquals(expected, cloudlets.size(), "Backward compatible method should work");
    }

    @Test
    public void testConfigValidationTriggered() {
        WorkloadConfig invalidConfig = new WorkloadConfig();
        invalidConfig.setPatternType("steady");
        invalidConfig.setSeed(42L);
        invalidConfig.setDuration(-1);  // Invalid: negative duration
        invalidConfig.setIntensity(5.0);

        assertThrows(IllegalArgumentException.class, () -> {
            generator.generateCloudlets(invalidConfig);
        }, "Invalid config should trigger validation exception");
    }

    @Test
    public void testHeterogeneousCloudletDistribution() {
        // Generate 1000 cloudlets for statistical significance
        WorkloadConfig config = new WorkloadConfig("steady", 42L, 100, 10.0);
        List<Cloudlet> cloudlets = generator.generateCloudlets(config);

        // Classify cloudlets by length into light/medium/heavy buckets
        // Note: CloudletFactory multiplier removed, so cloudlet length matches descriptor length
        int lightCount = 0, mediumCount = 0, heavyCount = 0;
        for (Cloudlet c : cloudlets) {
            long length = c.getLength();
            if (length >= 1000 && length < 5001) {
                lightCount++;
            } else if (length >= 10000 && length < 50001) {
                mediumCount++;
            } else if (length >= 100000 && length <= 200000) {
                heavyCount++;
            }
        }

        // Validate 70-20-10 distribution with ±5% tolerance
        double lightPct = (lightCount * 100.0) / cloudlets.size();
        double mediumPct = (mediumCount * 100.0) / cloudlets.size();
        double heavyPct = (heavyCount * 100.0) / cloudlets.size();

        assertTrue(lightPct >= 65 && lightPct <= 75,
            String.format("Light cloudlets should be ~70%% (got %.1f%%)", lightPct));
        assertTrue(mediumPct >= 15 && mediumPct <= 25,
            String.format("Medium cloudlets should be ~20%% (got %.1f%%)", mediumPct));
        assertTrue(heavyPct >= 5 && heavyPct <= 15,
            String.format("Heavy cloudlets should be ~10%% (got %.1f%%)", heavyPct));

        // Test reproducibility with same seed
        List<Cloudlet> cloudlets2 = generator.generateCloudlets(config);
        WorkloadValidator validator = new WorkloadValidator();
        int hash1 = validator.computeWorkloadHash(cloudlets);
        int hash2 = validator.computeWorkloadHash(cloudlets2);

        assertEquals(hash1, hash2, "Same seed should produce identical distribution");
    }
}
