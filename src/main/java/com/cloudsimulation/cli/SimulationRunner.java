package com.cloudsimulation.cli;

import com.cloudsimulation.algorithms.LoadBalancingPolicy;
import com.cloudsimulation.algorithms.PolicyManager;
import com.cloudsimulation.algorithms.threshold.ThresholdBalancer;
import com.cloudsimulation.algorithms.threshold.ThresholdConfig;
import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import com.cloudsimulation.io.CSVExporter;
import com.cloudsimulation.metrics.AlgorithmMetrics;
import com.cloudsimulation.metrics.HierarchicalCollector;
import com.cloudsimulation.metrics.MetricsCollector;
import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.LoadBalancingPlan;
import com.cloudsimulation.models.ScenarioConfig;
import com.cloudsimulation.models.SimulationResult;
import com.cloudsimulation.utils.RandomSeed;
import com.cloudsimulation.workload.WorkloadGenerator;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SimulationRunner orchestrates the entire simulation lifecycle from initialization
 * through execution to result tracking.
 *
 * Manages infrastructure setup, workload generation, simulation execution,
 * and metrics collection with resource cleanup.
 */
public class SimulationRunner {
    private static final Logger logger = LoggerFactory.getLogger(SimulationRunner.class);

    private final ScenarioConfig config;
    private CloudSimIntegration cloudSim;
    private List<Vm> createdVms = new ArrayList<>();  // VMs created for this simulation
    private CSVExporter csvExporter;  // CSV exporter for metrics
    private MetricsSnapshot lastSnapshot;  // Last collected metrics snapshot for result aggregation
    private PolicyManager policyManager;  // Manages load balancing policies
    private LoadBalancingPolicy activePolicy;  // Currently active policy for migrations

    /**
     * Creates a new SimulationRunner with the specified scenario configuration.
     *
     * @param config Scenario configuration containing infrastructure and simulation parameters
     */
    public SimulationRunner(ScenarioConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ScenarioConfig is required");
        }
        this.config = config;
        logger.info("SimulationRunner created for scenario: {}", config.getScenarioId());
    }

    /**
     * Executes the complete simulation workflow.
     * Handles infrastructure setup, workload generation, simulation execution,
     * metrics collection, and resource cleanup.
     *
     * @return SimulationResult containing aggregated metrics and execution data
     */
    public SimulationResult run() {
        logger.info("Starting simulation for scenario: {}", config.getScenarioId());

        try {
            // Initialize CSV exporter
            csvExporter = new CSVExporter();
            String runId = csvExporter.initializeRun(config.getScenarioId());
            logger.info("CSV export initialized for run: {}", runId);

            // Set random seed for reproducibility
            if (config.getSeed() != null) {
                RandomSeed.setSeed(config.getSeed());
                logger.info("Random seed set to: {}", config.getSeed());
            }

            // Step 1: Create CloudSim Plus simulation instance
            CloudSimPlus simulation = new CloudSimPlus();

            // Step 2: Build infrastructure using FederationBuilder
            List<Federation> federations = buildInfrastructure(simulation);
            logger.info("Infrastructure built: {} federation(s)", federations.size());

            // Step 3: Initialize CloudSimIntegration
            cloudSim = new CloudSimIntegration();
            double tickInterval = config.getTickInterval() != null ? config.getTickInterval() : 1.0;
            cloudSim.initialize(simulation, federations, tickInterval);
            logger.info("CloudSimIntegration initialized with tick interval: {}", tickInterval);

            // Step 3.5: Initialize load balancing policy
            initializePolicy();
            logger.info("Load balancing policy initialized: {}", activePolicy != null ? activePolicy.getName() : "none");

            // Step 4: Submit VMs that were created during infrastructure build
            cloudSim.submitVms(createdVms);
            logger.info("Submitted {} VMs to broker", createdVms.size());

            // Step 5: Generate workload using WorkloadGenerator
            WorkloadGenerator workloadGenerator = new WorkloadGenerator();
            int duration = config.getDuration() != null ? config.getDuration() : 100;
            List<Cloudlet> cloudlets;

            // Use WorkloadConfig if available, otherwise fall back to steady workload
            if (config.getWorkloadConfig() != null) {
                logger.info("Generating workload from config: pattern={}, intensity={}",
                    config.getWorkloadConfig().getPatternType(),
                    config.getWorkloadConfig().getIntensity());
                cloudlets = workloadGenerator.generateCloudlets(config.getWorkloadConfig());
            } else {
                logger.warn("No workloadConfig found, using default steady workload (50 cloudlets/tick)");
                cloudlets = workloadGenerator.generateSteadyWorkload(50, duration);
            }

            cloudSim.submitCloudlets(cloudlets);
            logger.info("Submitted {} cloudlets to broker", cloudlets.size());

            // Step 6: Schedule periodic monitoring events before starting simulation
            schedulePeriodicMonitoring(duration);

            // Step 7: Start simulation (runs to completion)
            cloudSim.start();
            logger.info("Simulation completed");

            // Step 8: Report final results
            int cloudletsProcessed = reportResults();

            // Step 9: Build and return simulation result
            SimulationResult result = buildSimulationResult(cloudletsProcessed);
            logger.info("Simulation completed successfully for scenario: {}", config.getScenarioId());

            return result;

        } catch (IOException e) {
            logger.error("Failed to initialize CSV export for scenario: {}", config.getScenarioId(), e);
            throw new RuntimeException("CSV export initialization failed", e);
        } catch (Exception e) {
            logger.error("Simulation failed for scenario: {}", config.getScenarioId(), e);
            throw new RuntimeException("Simulation execution failed", e);
        } finally {
            // Step 9: Clean up resources
            cleanup();
        }
    }

    /**
     * Initializes load balancing policy based on scenario configuration.
     * Creates PolicyManager, instantiates policy, and sets it as active.
     */
    private void initializePolicy() {
        policyManager = new PolicyManager();

        // Get policy name from config (default to "threshold" if not specified)
        String policyName = "threshold";  // Default policy
        if (config.getPolicyConfig() != null && config.getPolicyConfig().getDefaultPolicy() != null) {
            policyName = config.getPolicyConfig().getDefaultPolicy().toLowerCase();
        }

        logger.info("Creating load balancing policy: {}", policyName);

        // Create and register policy based on name
        LoadBalancingPolicy policy;
        switch (policyName) {
            case "threshold":
                // ThresholdBalancer needs ThresholdConfig and CloudSimIntegration
                ThresholdConfig thresholdConfig = new ThresholdConfig();  // Use default thresholds
                policy = new ThresholdBalancer(thresholdConfig, cloudSim);
                logger.info("ThresholdBalancer created with CPU upper={}, CPU lower={}, Memory upper={}, Memory lower={}",
                    thresholdConfig.getCpuUpperThreshold(),
                    thresholdConfig.getCpuLowerThreshold(),
                    thresholdConfig.getMemoryUpperThreshold(),
                    thresholdConfig.getMemoryLowerThreshold());
                break;
            case "noop":
            case "none":
                // NoOpPolicy for baseline comparisons
                policy = new com.cloudsimulation.algorithms.NoOpPolicy();
                break;
            default:
                logger.warn("Unknown policy '{}', defaulting to threshold", policyName);
                ThresholdConfig defaultConfig = new ThresholdConfig();
                policy = new ThresholdBalancer(defaultConfig, cloudSim);
                break;
        }

        policyManager.addPolicy(policy.getName(), policy);
        policyManager.setActivePolicy(policy.getName());
        activePolicy = policy;

        logger.info("Policy '{}' registered and activated", policy.getName());
    }

    /**
     * Builds federated infrastructure based on scenario configuration.
     *
     * @param simulation CloudSim Plus simulation instance
     * @return List of Federation instances
     */
    private List<Federation> buildInfrastructure(CloudSimPlus simulation) {
        List<Federation> federations = new ArrayList<>();

        ScenarioConfig.InfrastructureConfig infraConfig = config.getInfrastructureConfig();
        if (infraConfig == null) {
            throw new IllegalStateException("Infrastructure configuration is required");
        }

        int federationCount = infraConfig.getFederationCount() != null ? infraConfig.getFederationCount() : 1;
        int datacentersPerFed = infraConfig.getDatacentersPerFederation() != null ? infraConfig.getDatacentersPerFederation() : 2;
        int hostsPerDc = infraConfig.getHostsPerDatacenter() != null ? infraConfig.getHostsPerDatacenter() : 5;
        int vmsPerHost = infraConfig.getVmsPerHost() != null ? infraConfig.getVmsPerHost() : 2;

        logger.info("Building infrastructure: {} federations, {} datacenters/fed, {} hosts/dc, {} VMs/host",
                    federationCount, datacentersPerFed, hostsPerDc, vmsPerHost);

        // Build federations
        for (int f = 0; f < federationCount; f++) {
            Federation federation = new Federation("Federation-" + f);

            // Create FederationBuilder and configure it using builder pattern
            // Set vmsPerHost to 0 to prevent FederationBuilder from creating VMs
            // We'll create VMs separately to avoid broker conflicts
            FederationBuilder builder = new FederationBuilder(simulation);
            Federation builtFederation = builder
                .withDatacenters(datacentersPerFed)
                .withHostsPerDatacenter(hostsPerDc)
                .withVmsPerHost(0)  // Don't create VMs in builder
                .build();

            // Manually create VMs for this federation
            int totalHosts = datacentersPerFed * hostsPerDc;
            int totalVmsForFed = totalHosts * vmsPerHost;
            List<Vm> vmsForFed = builder.getVmFactory().createVms(totalVmsForFed);
            logger.debug("Created {} VMs for federation {}", vmsForFed.size(), f);

            // Store VMs in class field for later submission
            createdVms.addAll(vmsForFed);

            // Copy datacenters from built federation to our federation
            for (Datacenter dc : builtFederation.getDatacenters()) {
                federation.addDatacenter((int) dc.getId(), dc);
            }

            federations.add(federation);
        }

        return federations;
    }


    /**
     * Schedules periodic monitoring events for console output every 10 ticks.
     * Uses CloudSim Plus event scheduling to collect and display metrics during simulation.
     *
     * @param duration Simulation duration in seconds
     */
    private void schedulePeriodicMonitoring(int duration) {
        final MetricsCollector metricsCollector = new HierarchicalCollector();

        System.out.printf("=== SCHEDULING MONITORING for duration=%d seconds ===%n", duration);
        logger.info("Scheduling periodic monitoring for {} seconds", duration);

        // Use simple tick counter - CloudSim clock is unreliable with multiple datacenters
        final int[] tickCounter = new int[]{0};
        final int ticksToCollect = duration / 10;  // Collect every 10 seconds

        // Add a single clock tick listener
        cloudSim.getSimulation().addOnClockTickListener(evt -> {
            tickCounter[0]++;

            // Output every 10 clock tick events
            if (tickCounter[0] % 10 == 0 && tickCounter[0] / 10 <= ticksToCollect) {
                int currentTick = tickCounter[0];

                System.out.printf("[COUNTER] tick#%d (every 10 ticks)%n", currentTick);

                // Use elapsed time from CloudSimIntegration for timestamp
                double elapsedTime = cloudSim.getElapsedTime();
                MetricsSnapshot snapshot = metricsCollector.collect(cloudSim);
                lastSnapshot = snapshot;  // Store for final result aggregation
                double utilization = snapshot.getAvgCpuUtilization() * 100;

                System.out.printf("Tick %d: Utilization = %.2f%%\n", currentTick, utilization);
                logger.info("Tick {}: Utilization = {:.2f}%", currentTick, utilization);

                // Invoke load balancing policy to evaluate and execute migrations
                if (activePolicy != null) {
                    try {
                        LoadBalancingPlan plan = activePolicy.evaluate(snapshot);
                        if (plan != null && plan.getMigrationCount() > 0) {
                            logger.info("Policy '{}' generated migration plan with {} migrations at tick {}",
                                activePolicy.getName(), plan.getMigrationCount(), currentTick);
                            int successfulMigrations = cloudSim.executeMigrationPlan(plan);
                            logger.info("Executed {} out of {} migrations successfully",
                                successfulMigrations, plan.getMigrationCount());
                        } else {
                            logger.debug("No migrations needed at tick {}", currentTick);
                        }
                    } catch (Exception e) {
                        logger.error("Error executing load balancing policy at tick {}", currentTick, e);
                    }
                }

                // Export metrics to CSV
                try {
                    csvExporter.appendMetrics(snapshot);
                } catch (IOException e) {
                    logger.error("Failed to export metrics at tick {}", currentTick, e);
                }
            }
        });

        logger.info("Scheduled periodic monitoring for {} ticks", duration);
    }

    /**
     * Reports final simulation results including cloudlet completion and memory usage.
     *
     * @return Number of cloudlets processed
     */
    private int reportResults() {
        // Track cloudlet completion
        List<Cloudlet> finishedCloudlets = cloudSim.getBroker().getCloudletFinishedList();
        System.out.printf("Simulation complete: %d cloudlets processed\n", finishedCloudlets.size());
        logger.info("Cloudlets processed: {}", finishedCloudlets.size());

        // Check memory usage
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedMB = memoryUsed / (1024 * 1024);

        System.out.printf("Memory usage: %d MB\n", memoryUsedMB);
        logger.info("Memory usage: {} MB", memoryUsedMB);

        if (memoryUsedMB > 2048) {
            logger.warn("Memory usage exceeded 2GB limit: {} MB", memoryUsedMB);
        }

        return finishedCloudlets.size();
    }

    /**
     * Builds SimulationResult from collected metrics and execution data.
     *
     * @param cloudletsProcessed Number of cloudlets completed
     * @return SimulationResult with aggregated algorithm metrics
     */
    private SimulationResult buildSimulationResult(int cloudletsProcessed) {
        // Get memory usage
        Runtime runtime = Runtime.getRuntime();
        long memoryUsed = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsedMB = memoryUsed / (1024 * 1024);

        // Convert MetricsSnapshot to AlgorithmMetrics
        AlgorithmMetrics algorithmMetrics;
        if (lastSnapshot != null) {
            String policyName = config.getPolicyConfig() != null ?
                config.getPolicyConfig().getDefaultPolicy() : "unknown";

            algorithmMetrics = new AlgorithmMetrics.Builder()
                .algorithmName(policyName)
                .timestamp(lastSnapshot.getTimestamp())
                .totalMigrations(lastSnapshot.getTotalMigrations())
                .overloadEvents(lastSnapshot.getOverloadedVmCount())
                .averageUtilization(lastSnapshot.getAvgCpuUtilization())
                .slaViolations(0)  // TODO: Calculate from cloudlet failures
                .decisionTimeMs(0)  // TODO: Track decision time during simulation
                .build();
        } else {
            // Fallback if no metrics collected
            String policyName = config.getPolicyConfig() != null ?
                config.getPolicyConfig().getDefaultPolicy() : "unknown";
            algorithmMetrics = new AlgorithmMetrics.Builder()
                .algorithmName(policyName)
                .build();
        }

        return new SimulationResult(
            config.getScenarioId(),
            algorithmMetrics,
            cloudletsProcessed,
            memoryUsedMB,
            true  // success = true if we reached this point
        );
    }

    /**
     * Cleans up simulation resources.
     * Should be called in finally block to ensure proper cleanup.
     */
    private void cleanup() {
        // Close CSV exporter first to ensure all data is written
        if (csvExporter != null) {
            try {
                csvExporter.close();
                logger.info("CSV exporter closed");
            } catch (Exception e) {
                logger.error("Error closing CSV exporter", e);
            }
        }

        if (cloudSim != null) {
            try {
                cloudSim.terminate();
                logger.info("Simulation resources cleaned up");
            } catch (Exception e) {
                logger.error("Error during cleanup", e);
            }
        }
    }
}
