package com.cloudsimulation.cli;

import com.cloudsimulation.io.YAMLConfigLoader;
import com.cloudsimulation.models.ScenarioBenchmarkResult;
import com.cloudsimulation.models.ScenarioConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Command-line interface for the Cloud Simulation Load Balancing system.
 * Handles argument parsing and delegates to appropriate execution modes.
 */
public class CommandLineInterface {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineInterface.class);
    private static final String DEFAULT_BENCHMARKS_DIR = "configs/benchmarks";

    /**
     * Executes the CLI with provided arguments.
     *
     * @param args Command-line arguments
     * @return Exit code (0 = success, 1 = failure)
     */
    public int execute(String[] args) {
        if (args.length == 0) {
            printUsage();
            return 1;
        }

        try {
            // Check for --benchmark-scenarios flag
            if (containsFlag(args, "--benchmark-scenarios")) {
                return executeBenchmarkScenarios(args);
            }

            // Default: single scenario execution
            return executeSingleScenario(args[0]);

        } catch (Exception e) {
            logger.error("Execution failed", e);
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }

    /**
     * Executes benchmark scenario suite.
     *
     * @param args Command-line arguments
     * @return Exit code
     */
    private int executeBenchmarkScenarios(String[] args) {
        logger.info("Executing benchmark scenario suite");

        // Parse --algorithms parameter (comma-separated list)
        List<String> algorithms = parseAlgorithmsParameter(args);
        if (algorithms.isEmpty()) {
            algorithms = getDefaultAlgorithms();
            logger.info("No algorithms specified, using defaults: {}", algorithms);
        }

        // Get benchmarks directory (optional --benchmarks-dir parameter)
        String benchmarksDir = getParameterValue(args, "--benchmarks-dir", DEFAULT_BENCHMARKS_DIR);

        System.out.println("========================================");
        System.out.println("Scenario Benchmark Suite");
        System.out.println("Benchmarks Directory: " + benchmarksDir);
        System.out.println("Algorithms: " + String.join(", ", algorithms));
        System.out.println("========================================\n");

        // Create and run benchmark runner
        ScenarioBenchmarkRunner runner = new ScenarioBenchmarkRunner(benchmarksDir);
        ScenarioBenchmarkResult result = runner.runScenarioBenchmarks(algorithms);

        // Generate and print master comparison matrix
        String matrix = runner.generateMasterMatrix(result);
        System.out.println(matrix);

        // Report summary
        System.out.println("\nBenchmark Suite Summary:");
        System.out.println("  Total Scenarios: " + result.getTotalScenariosRun());
        System.out.println("  Total Algorithms: " + result.getTotalAlgorithmsTested());
        System.out.println("  Total Runs: " + (result.getTotalScenariosRun() * result.getTotalAlgorithmsTested()));
        System.out.println("  Failures: " + result.getFailedExecutions().size());

        if (!result.getFailedExecutions().isEmpty()) {
            System.out.println("\nFailed Executions:");
            for (var failure : result.getFailedExecutions()) {
                System.out.println("  ✗ " + failure.getScenarioId() + " / " +
                    failure.getAlgorithmName() + ": " + failure.getFailureType());
            }
        }

        // Exit with code 1 if any failures, 0 if all passed
        if (!result.getFailedExecutions().isEmpty()) {
            logger.warn("Benchmark suite completed with {} failures", result.getFailedExecutions().size());
            return 1;
        }

        logger.info("Benchmark suite completed successfully");
        return 0;
    }

    /**
     * Executes single scenario simulation.
     *
     * @param configPath Path to YAML scenario file
     * @return Exit code
     */
    private int executeSingleScenario(String configPath) {
        logger.info("Executing single scenario: {}", configPath);

        ScenarioConfig config = YAMLConfigLoader.loadScenario(configPath);
        logger.info("Configuration loaded for scenario: {}", config.getScenarioId());

        System.out.println("========================================");
        System.out.println("Cloud Simulation Load Balancing");
        System.out.println("Scenario: " + config.getScenarioId());
        System.out.println("========================================\n");

        SimulationRunner runner = new SimulationRunner(config);
        runner.run();

        System.out.println("\n========================================");
        System.out.println("Simulation completed successfully");
        System.out.println("========================================");

        logger.info("Single scenario execution completed");
        return 0;
    }

    /**
     * Parses --algorithms parameter from command-line arguments.
     *
     * @param args Command-line arguments
     * @return List of algorithm names
     */
    private List<String> parseAlgorithmsParameter(String[] args) {
        String algorithmsParam = getParameterValue(args, "--algorithms", null);
        if (algorithmsParam == null) {
            return new ArrayList<>();
        }

        // Split by comma and trim whitespace
        return Arrays.stream(algorithmsParam.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * Gets default algorithm list (all registered policies).
     * TODO: Query PolicyManager for registered policies
     *
     * @return Default algorithm list
     */
    private List<String> getDefaultAlgorithms() {
        // For now, return threshold as default
        // In future, query PolicyManager.getRegisteredPolicies()
        return List.of("threshold");
    }

    /**
     * Checks if a flag is present in command-line arguments.
     *
     * @param args Command-line arguments
     * @param flag Flag to check (e.g., "--benchmark-scenarios")
     * @return true if flag is present
     */
    private boolean containsFlag(String[] args, String flag) {
        return Arrays.asList(args).contains(flag);
    }

    /**
     * Gets the value of a command-line parameter.
     *
     * @param args Command-line arguments
     * @param param Parameter name (e.g., "--algorithms")
     * @param defaultValue Default value if parameter not found
     * @return Parameter value or default
     */
    private String getParameterValue(String[] args, String param, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals(param)) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }

    /**
     * Prints usage information.
     */
    private void printUsage() {
        System.out.println("Usage:");
        System.out.println("  Single scenario:");
        System.out.println("    java -jar cloud-simulation.jar <scenario.yaml>");
        System.out.println();
        System.out.println("  Benchmark scenario suite:");
        System.out.println("    java -jar cloud-simulation.jar --benchmark-scenarios");
        System.out.println("      [--algorithms threshold,nsga2,hybrid]");
        System.out.println("      [--benchmarks-dir configs/benchmarks]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --benchmark-scenarios    Run all scenarios in benchmarks directory");
        System.out.println("  --algorithms             Comma-separated list of algorithms (default: all registered)");
        System.out.println("  --benchmarks-dir         Directory containing benchmark YAML files (default: configs/benchmarks)");
    }
}
