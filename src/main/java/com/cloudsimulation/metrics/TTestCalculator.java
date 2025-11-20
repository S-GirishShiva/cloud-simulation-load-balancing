package com.cloudsimulation.metrics;

import com.cloudsimulation.models.StatisticalTest;

import java.util.List;

/**
 * Performs two-sample t-test for statistical significance testing.
 * Uses pooled variance approach assuming equal variances.
 *
 * <p><b>Formula:</b></p>
 * <pre>
 * t = (mean1 - mean2) / SE
 * SE = sqrt(pooledVariance * (1/n1 + 1/n2))
 * pooledVariance = ((n1-1)*var1 + (n2-1)*var2) / (n1 + n2 - 2)
 * df = n1 + n2 - 2
 * </pre>
 */
public class TTestCalculator {

    /**
     * Performs two-sample t-test between two samples.
     *
     * <p>Assumptions:</p>
     * <ul>
     *   <li>Independent samples</li>
     *   <li>Approximately normal distributions</li>
     *   <li>Equal variances (pooled variance approach)</li>
     * </ul>
     *
     * @param sample1 First sample (e.g., baseline algorithm runs)
     * @param sample2 Second sample (e.g., candidate algorithm runs)
     * @param metricName Name of the metric being tested
     * @return StatisticalTest object with t-statistic, p-value, and significance
     * @throws IllegalArgumentException if samples are null, empty, or too small (n < 2)
     */
    public StatisticalTest performTTest(List<Double> sample1, List<Double> sample2, String metricName) {
        validateSamples(sample1, sample2);

        int n1 = sample1.size();
        int n2 = sample2.size();

        double mean1 = calculateMean(sample1);
        double mean2 = calculateMean(sample2);

        double var1 = calculateVariance(sample1, mean1);
        double var2 = calculateVariance(sample2, mean2);

        // Calculate pooled variance
        double pooledVariance = ((n1 - 1) * var1 + (n2 - 1) * var2) / (n1 + n2 - 2);

        // Calculate standard error
        double standardError = Math.sqrt(pooledVariance * (1.0 / n1 + 1.0 / n2));

        // Calculate t-statistic
        // Handle edge case: if standard error is 0 (identical samples), t-statistic is 0
        double tStatistic;
        if (standardError == 0.0 || Double.isNaN(standardError)) {
            tStatistic = 0.0;
        } else {
            tStatistic = (mean1 - mean2) / standardError;
        }

        // Degrees of freedom
        int df = n1 + n2 - 2;

        // Approximate p-value (two-tailed test)
        double pValue = approximatePValue(Math.abs(tStatistic), df);

        return new StatisticalTest(metricName, tStatistic, pValue, df, n1 + n2);
    }

    /**
     * Validates sample inputs for t-test.
     *
     * @param sample1 First sample
     * @param sample2 Second sample
     * @throws IllegalArgumentException if validation fails
     */
    private void validateSamples(List<Double> sample1, List<Double> sample2) {
        if (sample1 == null || sample2 == null) {
            throw new IllegalArgumentException("Samples cannot be null");
        }
        if (sample1.size() < 2 || sample2.size() < 2) {
            throw new IllegalArgumentException("Each sample must have at least 2 observations");
        }
    }

    /**
     * Calculates the mean of a sample.
     *
     * @param sample List of values
     * @return Sample mean
     */
    private double calculateMean(List<Double> sample) {
        return sample.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /**
     * Calculates the sample variance.
     * Uses n-1 denominator (unbiased estimator).
     *
     * @param sample List of values
     * @param mean Pre-calculated mean
     * @return Sample variance
     */
    private double calculateVariance(List<Double> sample, double mean) {
        double sumSquaredDiff = sample.stream()
            .mapToDouble(x -> Math.pow(x - mean, 2))
            .sum();
        return sumSquaredDiff / (sample.size() - 1);
    }

    /**
     * Approximates p-value from t-statistic and degrees of freedom.
     * Uses simplified lookup for common significance levels.
     *
     * <p><b>Note:</b> This is a simplified approximation.
     * For production use, consider Apache Commons Math TDistribution for exact p-values.</p>
     *
     * @param absTStatistic Absolute value of t-statistic
     * @param df Degrees of freedom
     * @return Approximate p-value for two-tailed test
     */
    private double approximatePValue(double absTStatistic, int df) {
        // Simplified approximation based on common critical values
        // For df >= 30, t-distribution approximates normal distribution

        if (df >= 30) {
            // Critical values for two-tailed test (normal approximation)
            if (absTStatistic < 1.96) return 0.10;  // Not significant at 0.05
            if (absTStatistic < 2.58) return 0.01;  // Significant at 0.05, not at 0.01
            return 0.001;  // Highly significant
        }

        // For small df, use conservative estimates
        if (absTStatistic < 2.0) return 0.10;
        if (absTStatistic < 2.5) return 0.05;
        if (absTStatistic < 3.0) return 0.01;
        return 0.001;
    }
}
