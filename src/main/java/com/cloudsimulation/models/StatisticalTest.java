package com.cloudsimulation.models;

/**
 * Represents results of a two-sample t-test for statistical significance.
 * Used to determine if differences between algorithm performances are statistically significant.
 */
public class StatisticalTest {
    private final String metricName;
    private final double tStatistic;
    private final double pValue;
    private final int degreesOfFreedom;
    private final boolean isSignificant;
    private final int sampleSize;

    /**
     * Creates a new StatisticalTest result.
     *
     * @param metricName Name of the metric being tested
     * @param tStatistic Calculated t-statistic
     * @param pValue Statistical significance (p-value, 0-1 range)
     * @param degreesOfFreedom Degrees of freedom for the test
     * @param sampleSize Total sample size (n1 + n2)
     */
    public StatisticalTest(String metricName, double tStatistic, double pValue,
                          int degreesOfFreedom, int sampleSize) {
        this.metricName = metricName;
        this.tStatistic = tStatistic;
        this.pValue = pValue;
        this.degreesOfFreedom = degreesOfFreedom;
        this.sampleSize = sampleSize;
        this.isSignificant = pValue < 0.05; // Standard significance level (α = 0.05)
    }

    /**
     * Gets the metric name.
     *
     * @return Metric identifier
     */
    public String getMetricName() {
        return metricName;
    }

    /**
     * Gets the t-statistic.
     *
     * @return Calculated t-statistic value
     */
    public double getTStatistic() {
        return tStatistic;
    }

    /**
     * Gets the p-value.
     *
     * @return Statistical significance value (0-1)
     */
    public double getPValue() {
        return pValue;
    }

    /**
     * Gets the degrees of freedom.
     *
     * @return Degrees of freedom (n1 + n2 - 2)
     */
    public int getDegreesOfFreedom() {
        return degreesOfFreedom;
    }

    /**
     * Checks if the result is statistically significant.
     *
     * @return true if p-value < 0.05, false otherwise
     */
    public boolean isSignificant() {
        return isSignificant;
    }

    /**
     * Gets the total sample size.
     *
     * @return Combined sample size
     */
    public int getSampleSize() {
        return sampleSize;
    }

    @Override
    public String toString() {
        return String.format("StatisticalTest{metric='%s', t=%.3f, p=%.4f, df=%d, significant=%s}",
            metricName, tStatistic, pValue, degreesOfFreedom, isSignificant);
    }
}
