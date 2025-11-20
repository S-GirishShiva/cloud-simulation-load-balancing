package com.cloudsimulation.algorithms;

import com.cloudsimulation.algorithms.threshold.ThresholdConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ThresholdConfig class.
 * Tests default values, custom configuration, validation, and toString().
 */
public class ThresholdConfigTest {
    private ThresholdConfig config;

    @BeforeEach
    public void setup() {
        config = new ThresholdConfig();
    }

    @Test
    public void testDefaultThresholdValues() {
        assertEquals(0.8, config.getCpuUpperThreshold(), 0.001,
                    "Default CPU upper threshold should be 0.8");
        assertEquals(0.7, config.getCpuLowerThreshold(), 0.001,
                    "Default CPU lower threshold should be 0.7");
        assertEquals(0.8, config.getMemoryUpperThreshold(), 0.001,
                    "Default memory upper threshold should be 0.8");
        assertEquals(0.7, config.getMemoryLowerThreshold(), 0.001,
                    "Default memory lower threshold should be 0.7");
    }

    @Test
    public void testCustomThresholdConfiguration() {
        config.setCpuUpperThreshold(0.9);
        config.setCpuLowerThreshold(0.6);
        config.setMemoryUpperThreshold(0.85);
        config.setMemoryLowerThreshold(0.65);

        assertEquals(0.9, config.getCpuUpperThreshold(), 0.001);
        assertEquals(0.6, config.getCpuLowerThreshold(), 0.001);
        assertEquals(0.85, config.getMemoryUpperThreshold(), 0.001);
        assertEquals(0.65, config.getMemoryLowerThreshold(), 0.001);
    }

    @Test
    public void testValidationRejectsInvalidCpuThresholds() {
        config.setCpuUpperThreshold(0.5);
        config.setCpuLowerThreshold(0.7);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.validate(),
            "Should reject CPU upper threshold less than lower threshold"
        );

        assertTrue(exception.getMessage().contains("CPU upper threshold"));
        assertTrue(exception.getMessage().contains("must be greater than"));
    }

    @Test
    public void testValidationRejectsEqualCpuThresholds() {
        config.setCpuUpperThreshold(0.7);
        config.setCpuLowerThreshold(0.7);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.validate(),
            "Should reject equal CPU thresholds"
        );

        assertTrue(exception.getMessage().contains("CPU upper threshold"));
    }

    @Test
    public void testValidationRejectsInvalidMemoryThresholds() {
        config.setMemoryUpperThreshold(0.6);
        config.setMemoryLowerThreshold(0.8);

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.validate(),
            "Should reject memory upper threshold less than lower threshold"
        );

        assertTrue(exception.getMessage().contains("Memory upper threshold"));
        assertTrue(exception.getMessage().contains("must be greater than"));
    }

    @Test
    public void testSetterRejectsNegativeThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.setCpuUpperThreshold(-0.1),
            "Should reject negative CPU upper threshold"
        );

        assertTrue(exception.getMessage().contains("must be between 0.0 and 1.0"));
    }

    @Test
    public void testSetterRejectsThresholdAboveOne() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.setCpuLowerThreshold(1.5),
            "Should reject CPU lower threshold above 1.0"
        );

        assertTrue(exception.getMessage().contains("must be between 0.0 and 1.0"));
    }

    @Test
    public void testSetterRejectsNegativeMemoryThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.setMemoryUpperThreshold(-0.5),
            "Should reject negative memory upper threshold"
        );

        assertTrue(exception.getMessage().contains("must be between 0.0 and 1.0"));
    }

    @Test
    public void testSetterRejectsMemoryThresholdAboveOne() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> config.setMemoryLowerThreshold(2.0),
            "Should reject memory lower threshold above 1.0"
        );

        assertTrue(exception.getMessage().contains("must be between 0.0 and 1.0"));
    }

    @Test
    public void testConstructorWithValidThresholds() {
        ThresholdConfig customConfig = new ThresholdConfig(0.9, 0.75, 0.85, 0.7);

        assertEquals(0.9, customConfig.getCpuUpperThreshold(), 0.001);
        assertEquals(0.75, customConfig.getCpuLowerThreshold(), 0.001);
        assertEquals(0.85, customConfig.getMemoryUpperThreshold(), 0.001);
        assertEquals(0.7, customConfig.getMemoryLowerThreshold(), 0.001);
    }

    @Test
    public void testConstructorRejectsInvalidThresholds() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new ThresholdConfig(0.6, 0.8, 0.9, 0.7),
            "Constructor should reject invalid CPU thresholds"
        );

        assertTrue(exception.getMessage().contains("CPU upper threshold"));
    }

    @Test
    public void testToStringOutput() {
        String output = config.toString();

        assertNotNull(output, "toString() should not return null");
        assertTrue(output.contains("ThresholdConfig"), "toString() should contain class name");
        assertTrue(output.contains("0.80"), "toString() should contain CPU upper threshold");
        assertTrue(output.contains("0.70"), "toString() should contain CPU lower threshold");
    }

    @Test
    public void testValidationAcceptsValidConfiguration() {
        config.setCpuUpperThreshold(0.9);
        config.setCpuLowerThreshold(0.7);
        config.setMemoryUpperThreshold(0.85);
        config.setMemoryLowerThreshold(0.65);

        assertDoesNotThrow(() -> config.validate(),
                          "Validation should pass for valid configuration");
    }

    @Test
    public void testBoundaryValuesZero() {
        config.setCpuLowerThreshold(0.0);
        config.setMemoryLowerThreshold(0.0);

        assertEquals(0.0, config.getCpuLowerThreshold(), 0.001);
        assertEquals(0.0, config.getMemoryLowerThreshold(), 0.001);
    }

    @Test
    public void testBoundaryValuesOne() {
        config.setCpuUpperThreshold(1.0);
        config.setMemoryUpperThreshold(1.0);

        assertEquals(1.0, config.getCpuUpperThreshold(), 0.001);
        assertEquals(1.0, config.getMemoryUpperThreshold(), 0.001);
    }
}
