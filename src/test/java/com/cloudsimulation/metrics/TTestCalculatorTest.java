package com.cloudsimulation.metrics;

import com.cloudsimulation.models.StatisticalTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TTestCalculator.
 * Tests statistical significance calculation using two-sample t-test.
 */
public class TTestCalculatorTest {
    private TTestCalculator calculator;

    @BeforeEach
    public void setup() {
        calculator = new TTestCalculator();
    }

    @Test
    public void testPerformTTestWithIdenticalSamples() {
        List<Double> sample1 = List.of(10.0, 10.0, 10.0);
        List<Double> sample2 = List.of(10.0, 10.0, 10.0);

        StatisticalTest result = calculator.performTTest(sample1, sample2, "test_metric");

        assertEquals(0.0, result.getTStatistic(), 0.01, "T-statistic should be 0 for identical samples");
        assertTrue(result.getPValue() >= 0.05, "P-value should be high for identical samples");
        assertFalse(result.isSignificant(), "Result should not be significant");
        assertEquals("test_metric", result.getMetricName());
        assertEquals(6, result.getSampleSize());
    }

    @Test
    public void testPerformTTestWithDifferentSamples() {
        List<Double> sample1 = List.of(10.0, 12.0, 11.0, 9.0, 10.5);
        List<Double> sample2 = List.of(20.0, 22.0, 21.0, 19.0, 20.5);

        StatisticalTest result = calculator.performTTest(sample1, sample2, "test_metric");

        assertTrue(Math.abs(result.getTStatistic()) > 2.0, "T-statistic should be large for different samples");
        assertTrue(result.getPValue() < 0.10, "P-value should be low for different samples");
        assertTrue(result.isSignificant(), "Result should be significant");
        assertEquals(8, result.getDegreesOfFreedom(), "df should be n1 + n2 - 2 = 8");
    }

    @Test
    public void testPerformTTestWithSmallSamples() {
        List<Double> sample1 = List.of(10.0, 12.0, 11.0);
        List<Double> sample2 = List.of(15.0, 17.0, 16.0);

        StatisticalTest result = calculator.performTTest(sample1, sample2, "small_sample");

        assertNotNull(result);
        assertEquals(4, result.getDegreesOfFreedom(), "df = 3 + 3 - 2 = 4");
        assertEquals(6, result.getSampleSize());
        assertTrue(result.getPValue() > 0.0 && result.getPValue() <= 1.0, "P-value should be in valid range");
    }

    @Test
    public void testPerformTTestValidatesNullSamples() {
        List<Double> valid = List.of(10.0, 12.0);

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.performTTest(null, valid, "test");
        }, "Should throw exception for null sample1");

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.performTTest(valid, null, "test");
        }, "Should throw exception for null sample2");
    }

    @Test
    public void testPerformTTestValidatesSampleSize() {
        List<Double> tooSmall = List.of(10.0);
        List<Double> valid = List.of(10.0, 12.0);

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.performTTest(tooSmall, valid, "test");
        }, "Should throw exception for sample size < 2");

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.performTTest(valid, tooSmall, "test");
        }, "Should throw exception for sample size < 2");
    }

    @Test
    public void testPerformTTestValidatesEmptySamples() {
        List<Double> empty = List.of();
        List<Double> valid = List.of(10.0, 12.0);

        assertThrows(IllegalArgumentException.class, () -> {
            calculator.performTTest(empty, valid, "test");
        }, "Should throw exception for empty sample");
    }

    @Test
    public void testPerformTTestWithLargeSamples() {
        // Create two samples with 50 elements each
        List<Double> sample1 = generateSample(50, 10.0, 2.0);
        List<Double> sample2 = generateSample(50, 10.5, 2.0);

        StatisticalTest result = calculator.performTTest(sample1, sample2, "large_sample");

        assertNotNull(result);
        assertEquals(98, result.getDegreesOfFreedom(), "df = 50 + 50 - 2 = 98");
        assertEquals(100, result.getSampleSize());
        assertTrue(result.getPValue() > 0.0 && result.getPValue() <= 1.0);
    }

    @Test
    public void testIsSignificantFlagBasedOnPValue() {
        // Test with very different samples (should be significant)
        List<Double> sample1 = List.of(5.0, 6.0, 5.5, 5.2, 5.8);
        List<Double> sample2 = List.of(15.0, 16.0, 15.5, 15.2, 15.8);

        StatisticalTest result = calculator.performTTest(sample1, sample2, "test");

        assertTrue(result.getPValue() < 0.05, "P-value should be < 0.05");
        assertTrue(result.isSignificant(), "isSignificant should be true when p < 0.05");
    }

    @Test
    public void testStatisticalTestToString() {
        List<Double> sample1 = List.of(10.0, 12.0, 11.0);
        List<Double> sample2 = List.of(15.0, 17.0, 16.0);

        StatisticalTest result = calculator.performTTest(sample1, sample2, "migrations");

        String str = result.toString();
        assertTrue(str.contains("migrations"), "toString should contain metric name");
        assertTrue(str.contains("t="), "toString should contain t-statistic");
        assertTrue(str.contains("p="), "toString should contain p-value");
    }

    /**
     * Helper method to generate sample data.
     */
    private List<Double> generateSample(int size, double mean, double stdDev) {
        java.util.Random random = new java.util.Random(42);
        return java.util.stream.Stream.generate(() -> mean + random.nextGaussian() * stdDev)
            .limit(size)
            .toList();
    }
}
