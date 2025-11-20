package com.cloudsimulation.integration;

import com.cloudsimulation.models.WorkloadConfig;
import com.cloudsimulation.workload.WorkloadGenerator;
import com.cloudsimulation.workload.WorkloadValidator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for workload reproducibility.
 * Verifies that traffic_spike scenario produces identical results across multiple runs.
 */
public class ReproducibilityTest {
    private static final Logger logger = LoggerFactory.getLogger(ReproducibilityTest.class);

    private WorkloadGenerator generator;
    private WorkloadValidator validator;

    @BeforeEach
    public void setup() {
        generator = new WorkloadGenerator();
        validator = new WorkloadValidator();
    }

    @Test
    public void testTrafficSpikeScenarioReproducibility() {
        logger.info("Testing traffic_spike scenario reproducibility");

        // Load traffic_spike scenario configuration
        WorkloadConfig config = new WorkloadConfig("burst", 42L, 300, 5.0);
        config.setBurstMultiplier(10.0);

        // Run WorkloadGenerator 5 times with same seed
        List<Cloudlet> run1 = generator.generateCloudlets(config);
        List<Cloudlet> run2 = generator.generateCloudlets(config);
        List<Cloudlet> run3 = generator.generateCloudlets(config);
        List<Cloudlet> run4 = generator.generateCloudlets(config);
        List<Cloudlet> run5 = generator.generateCloudlets(config);

        // Verify all runs produce identical cloudlet sequences
        assertEquals(run1.size(), run2.size(), "Run 2 should have same count as Run 1");
        assertEquals(run1.size(), run3.size(), "Run 3 should have same count as Run 1");
        assertEquals(run1.size(), run4.size(), "Run 4 should have same count as Run 1");
        assertEquals(run1.size(), run5.size(), "Run 5 should have same count as Run 1");

        // Use WorkloadValidator to confirm consistency
        WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 5);

        assertTrue(result.isValid(), "Validation should pass - all runs should be identical");
        assertEquals(0, result.getDiscrepancies().size(), "No discrepancies expected");

        // Log validation summary
        logger.info("Traffic spike reproducibility test completed successfully");
        logger.info("Cloudlet count: {}", run1.size());
        logger.info("Validation result: {}", result.isValid() ? "PASSED" : "FAILED");
        logger.info("Discrepancies: {}", result.getDiscrepancies().size());
    }

    @Test
    public void testAllPatternsReproducibility() {
        logger.info("Testing all pattern types for reproducibility");

        String[] patterns = {"steady", "burst", "gradual_increase", "oscillating", "diurnal"};

        for (String pattern : patterns) {
            logger.info("Testing pattern: {}", pattern);

            WorkloadConfig config = new WorkloadConfig(pattern, 123L, 100, 3.0);
            if (pattern.equals("burst")) {
                config.setBurstMultiplier(8.0);
            }
            if (pattern.equals("gradual_increase")) {
                config.setRampUpDuration(50);
            }

            WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 5);

            assertTrue(result.isValid(), "Pattern '" + pattern + "' should be reproducible");
            assertEquals(0, result.getDiscrepancies().size(),
                "Pattern '" + pattern + "' should have no discrepancies");

            logger.info("Pattern '{}' validated successfully", pattern);
        }

        logger.info("All patterns reproducibility test completed successfully");
    }

    @Test
    public void testReproducibilityWithDifferentSeeds() {
        logger.info("Testing that different seeds produce different results");

        WorkloadConfig config1 = new WorkloadConfig("steady", 111L, 100, 5.0);
        WorkloadConfig config2 = new WorkloadConfig("steady", 222L, 100, 5.0);

        List<Cloudlet> cloudlets1 = generator.generateCloudlets(config1);
        List<Cloudlet> cloudlets2 = generator.generateCloudlets(config2);

        assertEquals(cloudlets1.size(), cloudlets2.size(), "Same params should produce same count");

        int hash1 = validator.computeWorkloadHash(cloudlets1);
        int hash2 = validator.computeWorkloadHash(cloudlets2);

        assertNotEquals(hash1, hash2, "Different seeds should produce different workloads");

        logger.info("Different seeds test completed successfully");
    }

    @Test
    public void testLargeScaleReproducibility() {
        logger.info("Testing reproducibility with large-scale workload");

        // Simulate full-day diurnal pattern
        WorkloadConfig config = new WorkloadConfig("diurnal", 999L, 86400, 5.0);

        WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 3);

        assertTrue(result.isValid(), "Large-scale workload should be reproducible");
        assertEquals(0, result.getDiscrepancies().size(), "No discrepancies in large-scale test");

        logger.info("Large-scale reproducibility test completed successfully");
    }
}
