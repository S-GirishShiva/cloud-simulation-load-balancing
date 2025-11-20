package com.cloudsimulation.io;

import com.cloudsimulation.models.ConfigValidationException;
import com.cloudsimulation.models.ScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.error.MarkedYAMLException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Parses YAML configuration files for scenario definitions and validates configuration before use.
 */
public class YAMLConfigLoader {
    private static final Logger logger = LoggerFactory.getLogger(YAMLConfigLoader.class);
    private static final int MAX_VMS = 1000;
    private static final int MAX_SIMULATION_DURATION = 100000;
    private static final int MAX_HOSTS_PER_DATACENTER = 100;
    private static final int MAX_FEDERATIONS = 10;

    /**
     * Loads and validates a scenario configuration from a YAML file.
     *
     * @param path Path to the YAML configuration file
     * @return Validated ScenarioConfig object
     * @throws ConfigValidationException if path is invalid, file not found, YAML is malformed, or validation fails
     */
    public static ScenarioConfig loadScenario(String path) {
        logger.info("Loading scenario configuration from: {}", path);

        // Path sanitization
        String sanitizedPath = sanitizePath(path);

        try (InputStream input = new FileInputStream(sanitizedPath)) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(new Constructor(ScenarioConfig.class, loaderOptions));
            ScenarioConfig config = yaml.load(input);

            logger.debug("YAML parsed successfully, validating configuration...");

            // Validate configuration
            validateConfig(config);

            logger.info("Configuration loaded and validated successfully: {}", config.getScenarioId());
            return config;

        } catch (FileNotFoundException e) {
            logger.error("Configuration file not found: {}", sanitizedPath);
            throw new ConfigValidationException(0, "path", sanitizedPath, "File not found");
        } catch (YAMLException e) {
            logger.error("YAML parsing error: {}", e.getMessage());
            int lineNumber = extractLineNumber(e);
            throw new ConfigValidationException(lineNumber, "yaml", "malformed", e.getMessage());
        } catch (ConfigValidationException e) {
            logger.error("Configuration validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            logger.error("Unexpected error loading configuration", e);
            throw new ConfigValidationException(0, "unknown", "unknown", e.getMessage());
        }
    }

    /**
     * Sanitizes file path to prevent directory traversal attacks.
     *
     * @param path File path to sanitize
     * @return Sanitized path
     * @throws ConfigValidationException if path contains directory traversal or is outside allowed directories
     */
    private static String sanitizePath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new ConfigValidationException(0, "path", "null/empty", "Path cannot be null or empty");
        }

        // Reject paths with directory traversal attempts
        if (path.contains("..")) {
            throw new ConfigValidationException(0, "path", path, "Directory traversal not allowed");
        }

        // Normalize path
        Path normalizedPath = Paths.get(path).normalize();
        String normalizedStr = normalizedPath.toString().replace("\\", "/");

        // Ensure path is within allowed directories (configs/ or src/test/resources/)
        if (!normalizedStr.startsWith("configs") && !normalizedStr.contains("src/test/resources")) {
            throw new ConfigValidationException(0, "path", path, "Path must be in configs/ or test resources directory");
        }

        return normalizedPath.toString();
    }

    /**
     * Validates scenario configuration fields and resource limits.
     *
     * @param config Configuration to validate
     * @throws ConfigValidationException if any validation constraint is violated
     */
    static void validateConfig(ScenarioConfig config) {
        if (config == null) {
            throw new ConfigValidationException(0, "config", "null", "Configuration cannot be null");
        }

        // Validate scenarioId
        if (config.getScenarioId() == null || config.getScenarioId().trim().isEmpty()) {
            throw new ConfigValidationException(0, "scenarioId", "null/empty", "Scenario ID cannot be null or empty");
        }
        if (!config.getScenarioId().matches("[a-zA-Z0-9_-]+")) {
            throw new ConfigValidationException(0, "scenarioId", config.getScenarioId(),
                    "Scenario ID must match pattern [a-zA-Z0-9_-]+");
        }

        // Validate duration
        if (config.getDuration() == null) {
            throw new ConfigValidationException(0, "duration", "null", "Duration cannot be null");
        }
        if (config.getDuration() <= 0) {
            throw new ConfigValidationException(0, "duration", config.getDuration().toString(),
                    "Duration must be greater than 0");
        }
        if (config.getDuration() > MAX_SIMULATION_DURATION) {
            throw new ConfigValidationException(0, "duration", config.getDuration().toString(),
                    "Duration exceeds resource limit of " + MAX_SIMULATION_DURATION + " seconds");
        }

        // Validate seed
        if (config.getSeed() == null) {
            throw new ConfigValidationException(0, "seed", "null", "Seed cannot be null");
        }

        // Validate tickInterval
        if (config.getTickInterval() == null) {
            throw new ConfigValidationException(0, "tickInterval", "null", "Tick interval cannot be null");
        }
        if (config.getTickInterval() <= 0) {
            throw new ConfigValidationException(0, "tickInterval", config.getTickInterval().toString(),
                    "Tick interval must be greater than 0");
        }
        if (config.getTickInterval() > 10.0) {
            throw new ConfigValidationException(0, "tickInterval", config.getTickInterval().toString(),
                    "Tick interval must be <= 10.0 seconds");
        }

        // Validate infrastructureConfig
        if (config.getInfrastructureConfig() == null) {
            throw new ConfigValidationException(0, "infrastructureConfig", "null",
                    "Infrastructure configuration cannot be null");
        }

        ScenarioConfig.InfrastructureConfig infra = config.getInfrastructureConfig();

        // Validate federationCount
        if (infra.getFederationCount() == null) {
            throw new ConfigValidationException(0, "federationCount", "null", "Federation count cannot be null");
        }
        if (infra.getFederationCount() < 1) {
            throw new ConfigValidationException(0, "federationCount", infra.getFederationCount().toString(),
                    "Federation count must be >= 1");
        }
        if (infra.getFederationCount() > MAX_FEDERATIONS) {
            throw new ConfigValidationException(0, "federationCount", infra.getFederationCount().toString(),
                    "Federation count exceeds resource limit of " + MAX_FEDERATIONS);
        }

        // Validate datacentersPerFederation
        if (infra.getDatacentersPerFederation() == null) {
            throw new ConfigValidationException(0, "datacentersPerFederation", "null",
                    "Datacenters per federation cannot be null");
        }
        if (infra.getDatacentersPerFederation() < 1) {
            throw new ConfigValidationException(0, "datacentersPerFederation",
                    infra.getDatacentersPerFederation().toString(),
                    "Datacenters per federation must be >= 1");
        }
        if (infra.getDatacentersPerFederation() > 20) {
            throw new ConfigValidationException(0, "datacentersPerFederation",
                    infra.getDatacentersPerFederation().toString(),
                    "Datacenters per federation must be <= 20");
        }

        // Validate hostsPerDatacenter
        if (infra.getHostsPerDatacenter() == null) {
            throw new ConfigValidationException(0, "hostsPerDatacenter", "null",
                    "Hosts per datacenter cannot be null");
        }
        if (infra.getHostsPerDatacenter() < 1) {
            throw new ConfigValidationException(0, "hostsPerDatacenter", infra.getHostsPerDatacenter().toString(),
                    "Hosts per datacenter must be >= 1");
        }
        if (infra.getHostsPerDatacenter() > MAX_HOSTS_PER_DATACENTER) {
            throw new ConfigValidationException(0, "hostsPerDatacenter", infra.getHostsPerDatacenter().toString(),
                    "Hosts per datacenter exceeds resource limit of " + MAX_HOSTS_PER_DATACENTER);
        }

        // Validate vmsPerHost
        if (infra.getVmsPerHost() == null) {
            throw new ConfigValidationException(0, "vmsPerHost", "null", "VMs per host cannot be null");
        }
        if (infra.getVmsPerHost() < 1) {
            throw new ConfigValidationException(0, "vmsPerHost", infra.getVmsPerHost().toString(),
                    "VMs per host must be >= 1");
        }
        if (infra.getVmsPerHost() > 20) {
            throw new ConfigValidationException(0, "vmsPerHost", infra.getVmsPerHost().toString(),
                    "VMs per host must be <= 20");
        }

        // Calculate and validate total VMs
        long totalVms = (long) infra.getFederationCount() *
                infra.getDatacentersPerFederation() *
                infra.getHostsPerDatacenter() *
                infra.getVmsPerHost();

        if (totalVms > MAX_VMS) {
            throw new ConfigValidationException(0, "total VMs", String.valueOf(totalVms),
                    "Total VMs (" + totalVms + ") exceeds resource limit of " + MAX_VMS);
        }

        logger.debug("Configuration validation passed. Total VMs: {}", totalVms);
    }

    /**
     * Extracts line number from YAMLException if available.
     *
     * @param e YAMLException to extract line number from
     * @return Line number or 0 if not available
     */
    private static int extractLineNumber(YAMLException e) {
        if (e instanceof MarkedYAMLException) {
            MarkedYAMLException marked = (MarkedYAMLException) e;
            Mark mark = marked.getProblemMark();
            if (mark != null) {
                return mark.getLine() + 1; // SnakeYAML uses 0-based line numbers
            }
        }
        return 0;
    }
}
