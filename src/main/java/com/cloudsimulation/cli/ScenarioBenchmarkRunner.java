package com.cloudsimulation.cli;

import com.cloudsimulation.io.YAMLConfigLoader;
import com.cloudsimulation.metrics.AlgorithmMetrics;
import com.cloudsimulation.metrics.ComparisonReportGenerator;
import com.cloudsimulation.models.ComparisonReport;
import com.cloudsimulation.models.FailedScenarioExecution;
import com.cloudsimulation.models.ScenarioBenchmarkResult;
import com.cloudsimulation.models.ScenarioConfig;
import com.cloudsimulation.models.SimulationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Executes algorithm workload scenario benchmarks.
 * Runs multiple algorithms across multiple scenarios and aggregates results.
 *
 * NOTE: This is separate from com.cloudsimulation.benchmarks (performance benchmarks).
 * This class benchmarks algorithm effectiveness, not framework performance.
 */
public class ScenarioBenchmarkRunner {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioBenchmarkRunner.class);

    private final String benchmarksDirectory;
    private final int timeoutSeconds;
    private final ComparisonReportGenerator reportGenerator;

    /**
     * Creates a new ScenarioBenchmarkRunner.
     *
     * @param benchmarksDirectory Directory containing YAML scenario files
     */
    public ScenarioBenchmarkRunner(String benchmarksDirectory) {
        this.benchmarksDirectory = benchmarksDirectory;
        this.timeoutSeconds = 60; // 60-second timeout per scenario run
        this.reportGenerator = new ComparisonReportGenerator();
    }

    /**
     * Runs all benchmark scenarios with specified algorithms.
     *
     * @param algorithms List of algorithm names (e.g., ["threshold", "nsga2"])
     * @return ScenarioBenchmarkResult with all scenario results and failures
     */
    public ScenarioBenchmarkResult runScenarioBenchmarks(List<String> algorithms) {
        Map<String, ComparisonReport> scenarioReports = new HashMap<>();
        List<FailedScenarioExecution> failedExecutions = new ArrayList<>();

        // Load all YAML scenarios from benchmarks directory
        File benchmarksDir = new File(benchmarksDirectory);
        File[] scenarioFiles = benchmarksDir.listFiles((dir, name) -> name.endsWith(".yaml"));

        if (scenarioFiles == null || scenarioFiles.length == 0) {
            throw new IllegalStateException("No YAML scenarios found in " + benchmarksDirectory);
        }

        logger.info("Scenario Benchmark Suite: {} scenarios x {} algorithms = {} runs",
            scenarioFiles.length, algorithms.size(), scenarioFiles.length * algorithms.size());

        // Execute each scenario with each algorithm
        for (File scenarioFile : scenarioFiles) {
            String scenarioId = extractScenarioId(scenarioFile.getName());
            Map<String, List<AlgorithmMetrics>> runsPerAlgorithm = new HashMap<>();

            for (String algorithm : algorithms) {
                try {
                    logger.info("Running: {} with {}", scenarioId, algorithm);

                    // Execute with timeout protection
                    SimulationResult result = executeScenarioWithTimeout(
                        scenarioFile.getPath(), algorithm, timeoutSeconds);

                    // Extract metrics from result
                    AlgorithmMetrics metrics = result.getAlgorithmMetrics();
                    runsPerAlgorithm.put(algorithm, Collections.singletonList(metrics));

                } catch (TimeoutException e) {
                    logger.error("TIMEOUT: {} with {} exceeded {}s", scenarioId, algorithm, timeoutSeconds);
                    failedExecutions.add(new FailedScenarioExecution(
                        scenarioId, algorithm, "TIMEOUT",
                        "Execution exceeded " + timeoutSeconds + "s timeout",
                        System.currentTimeMillis()));
                } catch (Exception e) {
                    logger.error("CRASH: {} with {} failed: {}", scenarioId, algorithm, e.getMessage(), e);
                    failedExecutions.add(new FailedScenarioExecution(
                        scenarioId, algorithm, "CRASH",
                        e.getMessage(), System.currentTimeMillis()));
                }
            }

            // Generate comparison report for this scenario
            if (!runsPerAlgorithm.isEmpty()) {
                ComparisonReport report = reportGenerator.generateReport(scenarioId, runsPerAlgorithm);
                scenarioReports.put(scenarioId, report);
            }
        }

        return new ScenarioBenchmarkResult(
            scenarioReports, failedExecutions, scenarioFiles.length, algorithms.size());
    }

    /**
     * Executes single scenario with timeout enforcement.
     *
     * @param scenarioPath Path to YAML scenario file
     * @param algorithm Algorithm name
     * @param timeoutSec Maximum execution time
     * @return SimulationResult if successful
     * @throws TimeoutException if execution exceeds timeout
     * @throws Exception if simulation crashes
     */
    private SimulationResult executeScenarioWithTimeout(
            String scenarioPath, String algorithm, int timeoutSec)
            throws TimeoutException, Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            Future<SimulationResult> future = executor.submit(() -> {
                ScenarioConfig config = YAMLConfigLoader.loadScenario(scenarioPath);
                if (config.getPolicyConfig() != null) {
                    config.getPolicyConfig().setDefaultPolicy(algorithm);
                }
                SimulationRunner runner = new SimulationRunner(config);
                return runner.run();
            });

            return future.get(timeoutSec, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            throw e;
        } catch (ExecutionException e) {
            // Unwrap the actual exception from ExecutionException
            throw (Exception) e.getCause();
        } finally {
            executor.shutdownNow(); // CRITICAL: Always cleanup
        }
    }

    /**
     * Generates master comparison matrix across all scenarios.
     *
     * @param result Complete benchmark suite results
     * @return Formatted ASCII table string
     */
    public String generateMasterMatrix(ScenarioBenchmarkResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("=".repeat(120)).append("\n");
        sb.append("SCENARIO BENCHMARK SUITE - MASTER COMPARISON MATRIX\n");
        sb.append("=".repeat(120)).append("\n\n");

        Map<String, ComparisonReport> scenarioReports = result.getScenarioReports();
        List<FailedScenarioExecution> failures = result.getFailedExecutions();

        if (scenarioReports.isEmpty() && failures.isEmpty()) {
            sb.append("No results available.\n");
            return sb.toString();
        }

        // Extract unique scenario and algorithm names
        List<String> scenarios = new ArrayList<>(scenarioReports.keySet());
        Collections.sort(scenarios);

        // Get algorithm names from first scenario report
        List<String> algorithms = new ArrayList<>();
        if (!scenarioReports.isEmpty()) {
            ComparisonReport firstReport = scenarioReports.values().iterator().next();
            for (var algResult : firstReport.getAlgorithmResults()) {
                if (!algorithms.contains(algResult.getAlgorithmName())) {
                    algorithms.add(algResult.getAlgorithmName());
                }
            }
        }

        // Build header row
        sb.append(String.format("%-20s", "Scenario"));
        for (String algorithm : algorithms) {
            sb.append(String.format("| %-35s", algorithm));
        }
        sb.append("|\n");
        sb.append("-".repeat(120)).append("\n");

        // Build data rows
        for (String scenarioId : scenarios) {
            sb.append(String.format("%-20s", scenarioId));

            ComparisonReport report = scenarioReports.get(scenarioId);
            List<String> paretoFront = report.getParetoFront();

            for (String algorithm : algorithms) {
                // Check if this scenario+algorithm failed
                FailedScenarioExecution failure = findFailure(failures, scenarioId, algorithm);
                if (failure != null) {
                    sb.append(String.format("| ✗ %-32s", failure.getFailureType()));
                    continue;
                }

                // Find algorithm result
                var algResult = report.getAlgorithmResults().stream()
                    .filter(ar -> ar.getAlgorithmName().equals(algorithm))
                    .findFirst();

                if (algResult.isPresent()) {
                    var metrics = algResult.get().getAggregatedMetrics();
                    boolean isParetoOptimal = paretoFront.contains(algorithm);

                    String cellContent = String.format("M:%d O:%d U:%.0f%% S:%d %s",
                        metrics.getTotalMigrations(),
                        metrics.getOverloadEvents(),
                        metrics.getAverageUtilization() * 100,
                        metrics.getSlaViolations(),
                        isParetoOptimal ? "★" : "");

                    sb.append(String.format("| %-35s", cellContent));
                } else {
                    sb.append(String.format("| %-35s", "-"));
                }
            }
            sb.append("|\n");
        }

        sb.append("=".repeat(120)).append("\n");
        sb.append("Legend: M=Migrations, O=Overloads, U=Utilization, S=SLA Violations, ★=Pareto-optimal, ✗=Failed\n");
        sb.append(String.format("Total: %d scenarios, %d algorithms, %d failures\n",
            result.getTotalScenariosRun(), result.getTotalAlgorithmsTested(), failures.size()));
        sb.append("=".repeat(120)).append("\n");

        return sb.toString();
    }

    /**
     * Finds a failure for given scenario and algorithm combination.
     *
     * @param failures List of all failures
     * @param scenarioId Scenario identifier
     * @param algorithm Algorithm name
     * @return FailedScenarioExecution if found, null otherwise
     */
    private FailedScenarioExecution findFailure(List<FailedScenarioExecution> failures,
                                                String scenarioId, String algorithm) {
        return failures.stream()
            .filter(f -> f.getScenarioId().equals(scenarioId) && f.getAlgorithmName().equals(algorithm))
            .findFirst()
            .orElse(null);
    }

    /**
     * Extracts scenario ID from YAML filename.
     *
     * @param filename Filename (e.g., "steady_load.yaml")
     * @return Scenario ID (e.g., "steady_load")
     */
    private String extractScenarioId(String filename) {
        return filename.replace(".yaml", "").replace(".yml", "");
    }
}
