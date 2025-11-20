package com.cloudsimulation.workload;

import com.cloudsimulation.models.WorkloadConfig;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates workload reproducibility across multiple runs.
 * Uses hash-based comparison for efficient validation of cloudlet sequences.
 */
public class WorkloadValidator {
    private static final Logger logger = LoggerFactory.getLogger(WorkloadValidator.class);

    /**
     * Validates that workload generation is reproducible across multiple runs.
     * Generates workload N times with the same seed and verifies identical results.
     *
     * @param config Workload configuration
     * @param runs Number of validation runs
     * @return ValidationResult with reproducibility status
     */
    public ValidationResult validateReproducibility(WorkloadConfig config, int runs) {
        logger.info("Validating reproducibility with {} runs", runs);

        WorkloadGenerator generator = new WorkloadGenerator();
        List<Integer> hashes = new ArrayList<>();
        List<String> discrepancies = new ArrayList<>();

        // Generate workload multiple times
        int firstHash = 0;
        for (int i = 0; i < runs; i++) {
            List<Cloudlet> cloudlets = generator.generateCloudlets(config);
            int hash = computeWorkloadHash(cloudlets);
            hashes.add(hash);

            if (i == 0) {
                firstHash = hash;
            } else if (hash != firstHash) {
                discrepancies.add(String.format("Run %d hash mismatch: expected %d, got %d", i, firstHash, hash));
            }
        }

        boolean isValid = discrepancies.isEmpty();
        logger.info("Validation complete: {} - {} discrepancies",
            isValid ? "PASSED" : "FAILED", discrepancies.size());

        return new ValidationResult(isValid, discrepancies);
    }

    /**
     * Computes hash of workload for quick comparison.
     * Hash based on cloudlet count, lengths, file sizes, and output sizes.
     *
     * @param cloudlets List of cloudlets
     * @return Hash value
     */
    public int computeWorkloadHash(List<Cloudlet> cloudlets) {
        int hash = cloudlets.size();
        for (Cloudlet c : cloudlets) {
            hash = 31 * hash + (int) c.getLength();
            hash = 31 * hash + (int) c.getFileSize();
            hash = 31 * hash + (int) c.getOutputSize();
        }
        return hash;
    }

    /**
     * Result of workload validation.
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final List<String> discrepancies;

        public ValidationResult(boolean isValid, List<String> discrepancies) {
            this.isValid = isValid;
            this.discrepancies = discrepancies;
        }

        public boolean isValid() { return isValid; }
        public List<String> getDiscrepancies() { return discrepancies; }
    }
}
