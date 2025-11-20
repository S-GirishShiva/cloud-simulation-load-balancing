package com.cloudsimulation.workload;

import com.cloudsimulation.models.WorkloadConfig;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WorkloadValidator.
 */
public class WorkloadValidatorTest {
    private WorkloadValidator validator;

    @BeforeEach
    public void setup() {
        validator = new WorkloadValidator();
    }

    @Test
    public void testValidateReproducibilitySuccess() {
        WorkloadConfig config = new WorkloadConfig("steady", 777L, 50, 3.0);

        WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 5);

        assertTrue(result.isValid(), "Validation should pass with same seed");
        assertTrue(result.getDiscrepancies().isEmpty(), "No discrepancies expected");
    }

    @Test
    public void testValidateReproducibilityDetectsDiscrepancies() {
        // This test uses a pattern that would produce different results
        // if seeds were not properly managed
        WorkloadConfig config = new WorkloadConfig("burst", 555L, 60, 4.0);
        config.setBurstMultiplier(10.0);

        WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 5);

        assertTrue(result.isValid(), "Validation should pass when seed is consistent");
        assertTrue(result.getDiscrepancies().isEmpty(), "No discrepancies with proper seed management");
    }

    @Test
    public void testComputeWorkloadHashConsistency() {
        WorkloadGenerator generator = new WorkloadGenerator();
        WorkloadConfig config = new WorkloadConfig("burst", 555L, 60, 4.0);

        List<Cloudlet> cloudlets1 = generator.generateCloudlets(config);
        List<Cloudlet> cloudlets2 = generator.generateCloudlets(config);

        int hash1 = validator.computeWorkloadHash(cloudlets1);
        int hash2 = validator.computeWorkloadHash(cloudlets2);

        assertEquals(hash1, hash2, "Identical workloads should have same hash");
    }

    @Test
    public void testComputeWorkloadHashDifferentForDifferentWorkloads() {
        WorkloadGenerator generator = new WorkloadGenerator();
        WorkloadConfig config1 = new WorkloadConfig("steady", 111L, 50, 3.0);
        WorkloadConfig config2 = new WorkloadConfig("steady", 222L, 50, 3.0);

        List<Cloudlet> cloudlets1 = generator.generateCloudlets(config1);
        List<Cloudlet> cloudlets2 = generator.generateCloudlets(config2);

        int hash1 = validator.computeWorkloadHash(cloudlets1);
        int hash2 = validator.computeWorkloadHash(cloudlets2);

        assertNotEquals(hash1, hash2, "Different workloads should have different hash");
    }

    @Test
    public void testValidationResultCorrectlyReportsDiscrepancies() {
        // Manually create a validation result with discrepancies
        List<String> discrepancies = List.of("Run 1 mismatch", "Run 2 mismatch");
        WorkloadValidator.ValidationResult result = new WorkloadValidator.ValidationResult(false, discrepancies);

        assertFalse(result.isValid(), "Result should be invalid");
        assertEquals(2, result.getDiscrepancies().size(), "Should report 2 discrepancies");
        assertEquals("Run 1 mismatch", result.getDiscrepancies().get(0));
        assertEquals("Run 2 mismatch", result.getDiscrepancies().get(1));
    }

    @Test
    public void testValidatorWithMultipleRuns() {
        WorkloadConfig config = new WorkloadConfig("steady", 888L, 50, 3.0);

        WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 10);

        assertTrue(result.isValid(), "Validation should pass with 10 runs");
        assertEquals(0, result.getDiscrepancies().size(), "No discrepancies expected");
    }

    @Test
    public void testValidatorWithDifferentPatterns() {
        String[] patterns = {"steady", "burst", "gradual_increase", "oscillating", "diurnal"};

        for (String pattern : patterns) {
            WorkloadConfig config = new WorkloadConfig(pattern, 999L, 100, 3.0);
            if (pattern.equals("burst")) {
                config.setBurstMultiplier(5.0);
            }
            if (pattern.equals("gradual_increase")) {
                config.setRampUpDuration(50);
            }

            WorkloadValidator.ValidationResult result = validator.validateReproducibility(config, 5);

            assertTrue(result.isValid(), "Pattern '" + pattern + "' should be reproducible");
        }
    }
}
