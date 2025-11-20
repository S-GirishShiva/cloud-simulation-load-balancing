package com.cloudsimulation.io;

import com.cloudsimulation.models.ConfigValidationException;
import com.cloudsimulation.models.ScenarioConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for YAMLConfigLoader.
 * Tests configuration loading, validation, and error handling.
 */
class YAMLConfigLoaderTest {

    @Test
    void testLoadValidConfiguration() {
        // Load example_scenario.yaml successfully
        ScenarioConfig config = YAMLConfigLoader.loadScenario("configs/example_scenario.yaml");

        assertNotNull(config);
        assertEquals("example_scenario", config.getScenarioId());
        assertEquals(100, config.getDuration());
        assertEquals(123456789L, config.getSeed());
        assertEquals(1.0, config.getTickInterval());

        assertNotNull(config.getInfrastructureConfig());
        assertEquals(1, config.getInfrastructureConfig().getFederationCount());
        assertEquals(2, config.getInfrastructureConfig().getDatacentersPerFederation());
        assertEquals(10, config.getInfrastructureConfig().getHostsPerDatacenter());
        assertEquals(5, config.getInfrastructureConfig().getVmsPerHost());
    }

    @Test
    void testLoadValidTestScenario() {
        // Load valid test scenario
        ScenarioConfig config = YAMLConfigLoader.loadScenario("src/test/resources/test-scenarios/valid_scenario.yaml");

        assertNotNull(config);
        assertEquals("valid_test_scenario", config.getScenarioId());
        assertEquals(50, config.getDuration());
        assertEquals(987654321L, config.getSeed());
        assertEquals(0.5, config.getTickInterval());
    }

    @Test
    void testValidateScenarioIdNull() {
        ScenarioConfig config = new ScenarioConfig();
        config.setDuration(100);
        config.setSeed(123456789L);
        config.setTickInterval(1.0);

        ScenarioConfig.InfrastructureConfig infra = new ScenarioConfig.InfrastructureConfig();
        infra.setFederationCount(1);
        infra.setDatacentersPerFederation(1);
        infra.setHostsPerDatacenter(5);
        infra.setVmsPerHost(2);
        config.setInfrastructureConfig(infra);

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.validateConfig(config);
        });

        assertTrue(exception.getMessage().contains("scenarioId"));
        assertTrue(exception.getMessage().contains("null"));
    }

    @Test
    void testValidateDurationZero() {
        ScenarioConfig config = new ScenarioConfig();
        config.setScenarioId("test");
        config.setDuration(0);
        config.setSeed(123456789L);
        config.setTickInterval(1.0);

        ScenarioConfig.InfrastructureConfig infra = new ScenarioConfig.InfrastructureConfig();
        infra.setFederationCount(1);
        infra.setDatacentersPerFederation(1);
        infra.setHostsPerDatacenter(5);
        infra.setVmsPerHost(2);
        config.setInfrastructureConfig(infra);

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.validateConfig(config);
        });

        assertTrue(exception.getMessage().contains("duration"));
        assertTrue(exception.getMessage().contains("must be greater than 0"));
    }

    @Test
    void testValidateDurationExceedsLimit() {
        ScenarioConfig config = new ScenarioConfig();
        config.setScenarioId("test");
        config.setDuration(100001);
        config.setSeed(123456789L);
        config.setTickInterval(1.0);

        ScenarioConfig.InfrastructureConfig infra = new ScenarioConfig.InfrastructureConfig();
        infra.setFederationCount(1);
        infra.setDatacentersPerFederation(1);
        infra.setHostsPerDatacenter(5);
        infra.setVmsPerHost(2);
        config.setInfrastructureConfig(infra);

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.validateConfig(config);
        });

        assertTrue(exception.getMessage().contains("duration"));
        assertTrue(exception.getMessage().contains("exceeds resource limit"));
    }

    @Test
    void testValidateHostsExceedLimit() {
        ScenarioConfig config = new ScenarioConfig();
        config.setScenarioId("test");
        config.setDuration(100);
        config.setSeed(123456789L);
        config.setTickInterval(1.0);

        ScenarioConfig.InfrastructureConfig infra = new ScenarioConfig.InfrastructureConfig();
        infra.setFederationCount(1);
        infra.setDatacentersPerFederation(1);
        infra.setHostsPerDatacenter(101);
        infra.setVmsPerHost(2);
        config.setInfrastructureConfig(infra);

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.validateConfig(config);
        });

        assertTrue(exception.getMessage().contains("hostsPerDatacenter"));
        assertTrue(exception.getMessage().contains("exceeds resource limit"));
    }

    @Test
    void testValidateTotalVmsExceedLimit() {
        // This should throw during loadScenario because it validates after loading
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.loadScenario("src/test/resources/test-scenarios/invalid_vm_count.yaml");
        });

        assertTrue(exception.getMessage().contains("total VMs"));
        assertTrue(exception.getMessage().contains("exceeds resource limit"));
    }

    @Test
    void testErrorMessageFormat() {
        ConfigValidationException exception = new ConfigValidationException(
                5, "duration", "0", "must be greater than 0"
        );

        String message = exception.getMessage();
        assertTrue(message.contains("line 5"));
        assertTrue(message.contains("duration"));
        assertTrue(message.contains("'0'"));
        assertTrue(message.contains("must be greater than 0"));
    }

    @Test
    void testPathSanitizationRejectsTraversal() {
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.loadScenario("../../../etc/passwd");
        });

        assertTrue(exception.getMessage().contains("Directory traversal not allowed"));
    }

    @Test
    void testFileNotFound() {
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.loadScenario("configs/nonexistent.yaml");
        });

        assertTrue(exception.getMessage().contains("File not found"));
    }

    @Test
    void testMalformedYaml() {
        // Malformed YAML should throw exception
        Exception exception = assertThrows(Exception.class, () -> {
            YAMLConfigLoader.loadScenario("src/test/resources/test-scenarios/malformed.yaml");
        });

        // Should throw exception for malformed YAML
        assertNotNull(exception);
        assertNotNull(exception.getMessage());
    }

    @Test
    void testInvalidDurationFile() {
        // This should throw during loadScenario because it validates after loading
        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.loadScenario("src/test/resources/test-scenarios/invalid_duration.yaml");
        });

        assertTrue(exception.getMessage().contains("duration"));
    }

    @Test
    void testValidateScenarioIdInvalidPattern() {
        ScenarioConfig config = new ScenarioConfig();
        config.setScenarioId("invalid scenario with spaces");
        config.setDuration(100);
        config.setSeed(123456789L);
        config.setTickInterval(1.0);

        ScenarioConfig.InfrastructureConfig infra = new ScenarioConfig.InfrastructureConfig();
        infra.setFederationCount(1);
        infra.setDatacentersPerFederation(1);
        infra.setHostsPerDatacenter(5);
        infra.setVmsPerHost(2);
        config.setInfrastructureConfig(infra);

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.validateConfig(config);
        });

        assertTrue(exception.getMessage().contains("scenarioId"));
        assertTrue(exception.getMessage().contains("must match pattern"));
    }

    @Test
    void testValidateTickIntervalExceedsLimit() {
        ScenarioConfig config = new ScenarioConfig();
        config.setScenarioId("test");
        config.setDuration(100);
        config.setSeed(123456789L);
        config.setTickInterval(11.0);

        ScenarioConfig.InfrastructureConfig infra = new ScenarioConfig.InfrastructureConfig();
        infra.setFederationCount(1);
        infra.setDatacentersPerFederation(1);
        infra.setHostsPerDatacenter(5);
        infra.setVmsPerHost(2);
        config.setInfrastructureConfig(infra);

        ConfigValidationException exception = assertThrows(ConfigValidationException.class, () -> {
            YAMLConfigLoader.validateConfig(config);
        });

        assertTrue(exception.getMessage().contains("tickInterval"));
        assertTrue(exception.getMessage().contains("<= 10.0"));
    }
}
