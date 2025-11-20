package com.cloudsimulation.algorithms.threshold;

import com.cloudsimulation.algorithms.LoadBalancingPolicy;
import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.metrics.MetricsSnapshot;
import com.cloudsimulation.models.LoadBalancingPlan;
import com.cloudsimulation.models.MigrationAction;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Threshold-based load balancing algorithm.
 *
 * <p>Reactive policy that triggers VM migrations when host utilization exceeds
 * configured thresholds. Uses First Fit Decreasing (FFD) strategy for target
 * host selection with safety validation.</p>
 *
 * <p><b>Algorithm Behavior:</b></p>
 * <ol>
 *   <li>Identify overloaded hosts (CPU or memory > upper threshold)</li>
 *   <li>Select least loaded VM from overloaded host for migration</li>
 *   <li>Find target host using FFD strategy (most available capacity first)</li>
 *   <li>Validate migration safety (target stays below lower threshold)</li>
 *   <li>Create migration action if safe target found</li>
 * </ol>
 *
 * <p><b>First Fit Decreasing (FFD) Strategy:</b></p>
 * <ul>
 *   <li>Sort candidate hosts by available capacity (descending)</li>
 *   <li>Try hosts from most to least available capacity</li>
 *   <li>Minimizes fragmentation and increases placement success</li>
 * </ul>
 */
public class ThresholdBalancer implements LoadBalancingPolicy {
    private static final Logger logger = LoggerFactory.getLogger(ThresholdBalancer.class);
    private static final String POLICY_NAME = "threshold";
    private static final double MIGRATION_TIME_ESTIMATE = 10.0; // seconds

    private final ThresholdConfig config;
    private final CloudSimIntegration cloudSim;

    /**
     * Constructs a new ThresholdBalancer.
     *
     * @param config Threshold configuration
     * @param cloudSim CloudSim integration for accessing simulation entities
     * @throws NullPointerException if config or cloudSim is null
     */
    public ThresholdBalancer(ThresholdConfig config, CloudSimIntegration cloudSim) {
        if (config == null) {
            throw new NullPointerException("ThresholdConfig cannot be null");
        }
        if (cloudSim == null) {
            throw new NullPointerException("CloudSimIntegration cannot be null");
        }

        this.config = config;
        this.cloudSim = cloudSim;

        // Validate configuration
        config.validate();

        logger.info("ThresholdBalancer initialized with config: {}", config);
    }

    /**
     * Evaluates system state and returns load balancing decisions.
     *
     * <p>Analyzes all hosts in the simulation, identifies overloaded hosts,
     * and generates migration plans to balance load across the infrastructure.</p>
     *
     * @param snapshot Current system metrics (used for timestamp and logging)
     * @return LoadBalancingPlan containing migration decisions
     * @throws NullPointerException if snapshot is null
     */
    @Override
    public LoadBalancingPlan evaluate(MetricsSnapshot snapshot) {
        if (snapshot == null) {
            throw new NullPointerException("MetricsSnapshot cannot be null");
        }

        long startTime = System.currentTimeMillis();

        logger.info("Threshold balancer evaluating system state at t={}", snapshot.getTimestamp());
        logger.debug("Current metrics: avgCpu={}, avgMem={}, overloadedVms={}, activeVms={}",
                    String.format("%.2f", snapshot.getAvgCpuUtilization()),
                    String.format("%.2f", snapshot.getAvgMemoryUtilization()),
                    snapshot.getOverloadedVmCount(),
                    snapshot.getActiveVmCount());

        List<MigrationAction> migrations = new ArrayList<>();

        // Get all hosts from CloudSim
        List<Host> allHosts = getAllHosts();

        if (allHosts.isEmpty()) {
            logger.warn("No hosts found in simulation");
            return buildPlan(snapshot, migrations, System.currentTimeMillis() - startTime);
        }

        // Task 2 subtasks: Identify overloaded hosts and process migrations
        List<Host> overloadedHosts = identifyOverloadedHosts(allHosts);

        for (Host sourceHost : overloadedHosts) {
            // Select VM to migrate (least loaded)
            Vm vmToMigrate = selectVmForMigration(sourceHost);

            if (vmToMigrate == null) {
                logger.debug("No suitable VM found on overloaded host {}", sourceHost.getId());
                continue;
            }

            // Find target host using FFD
            Host targetHost = findTargetHost(allHosts, vmToMigrate, sourceHost);

            if (targetHost == null) {
                logger.warn("No suitable target host found for VM {} from host {}",
                           vmToMigrate.getId(), sourceHost.getId());
                continue;
            }

            // Validate migration safety
            if (!validateMigrationSafety(vmToMigrate, targetHost)) {
                logger.warn("Migration rejected: VM {} to host {} would exceed safety threshold",
                           vmToMigrate.getId(), targetHost.getId());
                continue;
            }

            // Create migration action
            String reason = determineReason(sourceHost);
            MigrationAction migration = new MigrationAction(
                (int) vmToMigrate.getId(),
                (int) sourceHost.getId(),
                (int) targetHost.getId(),
                MIGRATION_TIME_ESTIMATE,
                reason
            );

            migrations.add(migration);

            logger.info("Migration decision: VM {} from Host {} to Host {} (reason: {})",
                       vmToMigrate.getId(), sourceHost.getId(), targetHost.getId(), reason);
        }

        long computationTime = System.currentTimeMillis() - startTime;

        logger.info("Threshold balancer completed: {} migrations planned in {}ms",
                   migrations.size(), computationTime);

        return buildPlan(snapshot, migrations, computationTime);
    }

    @Override
    public String getName() {
        return POLICY_NAME;
    }

    @Override
    public void reset() {
        // Stateless algorithm - no state to reset
        logger.debug("ThresholdBalancer.reset() called (no-op for stateless algorithm)");
    }

    /**
     * Gets all hosts from CloudSim federations.
     *
     * @return List of all hosts in simulation
     */
    private List<Host> getAllHosts() {
        List<Host> allHosts = new ArrayList<>();

        try {
            List<Federation> federations = cloudSim.getFederations();

            for (Federation federation : federations) {
                for (Datacenter datacenter : federation.getDatacenters()) {
                    allHosts.addAll(datacenter.getHostList());
                }
            }
        } catch (IllegalStateException e) {
            logger.error("Failed to get federations from CloudSim", e);
        }

        return allHosts;
    }

    /**
     * Identifies hosts that exceed CPU or memory upper thresholds.
     *
     * @param hosts List of all hosts
     * @return List of overloaded hosts
     */
    private List<Host> identifyOverloadedHosts(List<Host> hosts) {
        List<Host> overloadedHosts = new ArrayList<>();

        for (Host host : hosts) {
            double cpuUsage = host.getCpuPercentUtilization();
            double memoryUsage = calculateMemoryUtilization(host);

            boolean cpuOverloaded = cpuUsage > config.getCpuUpperThreshold();
            boolean memoryOverloaded = memoryUsage > config.getMemoryUpperThreshold();

            if (cpuOverloaded || memoryOverloaded) {
                overloadedHosts.add(host);

                if (cpuOverloaded) {
                    logger.info("Host {} exceeded CPU threshold: {:.2f} > {:.2f}",
                               host.getId(), cpuUsage, config.getCpuUpperThreshold());
                }
                if (memoryOverloaded) {
                    logger.info("Host {} exceeded memory threshold: {:.2f} > {:.2f}",
                               host.getId(), memoryUsage, config.getMemoryUpperThreshold());
                }
            }
        }

        logger.debug("Identified {} overloaded hosts out of {} total",
                    overloadedHosts.size(), hosts.size());

        return overloadedHosts;
    }

    /**
     * Selects the least loaded VM from overloaded host for migration.
     * Least loaded VMs are easier to place on target hosts.
     *
     * @param host Overloaded host
     * @return VM with lowest CPU utilization, or null if no VMs
     */
    private Vm selectVmForMigration(Host host) {
        List<Vm> vms = host.getVmList();

        if (vms.isEmpty()) {
            return null;
        }

        // Select VM with lowest CPU utilization (easier to place)
        Vm selectedVm = vms.stream()
            .min(Comparator.comparingDouble(Vm::getCpuPercentUtilization))
            .orElse(null);

        if (selectedVm != null) {
            logger.debug("Selected VM {} for migration (CPU: {:.2f})",
                        selectedVm.getId(), selectedVm.getCpuPercentUtilization());
        }

        return selectedVm;
    }

    /**
     * Finds target host using First Fit Decreasing (FFD) strategy.
     * Sorts hosts by available capacity (descending) and tries from most to least.
     *
     * @param allHosts List of all hosts
     * @param vm VM to migrate
     * @param sourceHost Source host (excluded from candidates)
     * @return Target host, or null if no suitable host found
     */
    private Host findTargetHost(List<Host> allHosts, Vm vm, Host sourceHost) {
        // Filter out source host and get candidate hosts
        List<Host> candidates = allHosts.stream()
            .filter(h -> h.getId() != sourceHost.getId())
            .collect(Collectors.toList());

        if (candidates.isEmpty()) {
            logger.debug("No candidate hosts available (only source host exists)");
            return null;
        }

        // Sort by available MIPS capacity (descending) - FFD strategy
        candidates.sort((h1, h2) -> {
            double available1 = h1.getTotalMipsCapacity() - h1.getTotalAllocatedMips();
            double available2 = h2.getTotalMipsCapacity() - h2.getTotalAllocatedMips();
            return Double.compare(available2, available1); // Descending order
        });

        // Try hosts from most to least available capacity
        for (Host candidateHost : candidates) {
            if (hasCapacity(candidateHost, vm)) {
                double availableMips = candidateHost.getTotalMipsCapacity() - candidateHost.getTotalAllocatedMips();

                logger.info("Target host {} selected (available capacity: {:.0f} MIPS)",
                           candidateHost.getId(), availableMips);

                return candidateHost;
            } else {
                logger.debug("Host {} lacks capacity for VM {}",
                            candidateHost.getId(), vm.getId());
            }
        }

        logger.debug("No suitable target host found with sufficient capacity");
        return null;
    }

    /**
     * Validates that migration is safe - target host will remain below lower threshold.
     *
     * @param vm VM to migrate
     * @param targetHost Target host
     * @return true if migration is safe, false otherwise
     */
    private boolean validateMigrationSafety(Vm vm, Host targetHost) {
        // Calculate projected CPU utilization after migration
        double targetCurrentCpu = targetHost.getCpuPercentUtilization();
        double vmCpuLoad = vm.getCpuPercentUtilization();

        // Projected CPU (simplified: add VM's CPU usage to target)
        double projectedCpu = targetCurrentCpu + vmCpuLoad;

        // Calculate projected memory utilization
        double targetCurrentMemory = calculateMemoryUtilization(targetHost);
        long vmMemory = vm.getRam().getCapacity();
        long targetTotalMemory = targetHost.getRam().getCapacity();

        double projectedMemory = targetCurrentMemory + ((double) vmMemory / targetTotalMemory);

        // Migration is safe if projected usage stays below lower threshold
        boolean cpuSafe = projectedCpu < config.getCpuLowerThreshold();
        boolean memorySafe = projectedMemory < config.getMemoryLowerThreshold();

        if (!cpuSafe) {
            logger.debug("CPU safety check failed: projected {:.2f} >= threshold {:.2f}",
                        projectedCpu, config.getCpuLowerThreshold());
        }

        if (!memorySafe) {
            logger.debug("Memory safety check failed: projected {:.2f} >= threshold {:.2f}",
                        projectedMemory, config.getMemoryLowerThreshold());
        }

        return cpuSafe && memorySafe;
    }

    /**
     * Checks if host has sufficient capacity for VM.
     *
     * @param host Target host
     * @param vm VM to check
     * @return true if host has capacity, false otherwise
     */
    private boolean hasCapacity(Host host, Vm vm) {
        // Check MIPS capacity
        double availableMips = host.getTotalMipsCapacity() - host.getTotalAllocatedMips();
        if (availableMips < vm.getTotalMipsCapacity()) {
            return false;
        }

        // Check RAM capacity
        long availableRam = host.getRam().getAvailableResource();
        if (availableRam < vm.getRam().getCapacity()) {
            return false;
        }

        // Check bandwidth capacity
        long availableBw = host.getBw().getAvailableResource();
        if (availableBw < vm.getBw().getCapacity()) {
            return false;
        }

        // Check storage capacity
        long availableStorage = host.getStorage().getAvailableResource();
        if (availableStorage < vm.getStorage().getCapacity()) {
            return false;
        }

        return true;
    }

    /**
     * Calculates memory utilization for a host.
     *
     * @param host Host to calculate utilization for
     * @return Memory utilization (0-1 range)
     */
    private double calculateMemoryUtilization(Host host) {
        long totalRam = host.getRam().getCapacity();
        long usedRam = host.getRam().getAllocatedResource();

        if (totalRam == 0) {
            return 0.0;
        }

        return (double) usedRam / totalRam;
    }

    /**
     * Determines migration reason based on host state.
     *
     * @param host Overloaded host
     * @return Migration reason string
     */
    private String determineReason(Host host) {
        double cpuUsage = host.getCpuPercentUtilization();
        double memoryUsage = calculateMemoryUtilization(host);

        boolean cpuOverloaded = cpuUsage > config.getCpuUpperThreshold();
        boolean memoryOverloaded = memoryUsage > config.getMemoryUpperThreshold();

        if (cpuOverloaded && memoryOverloaded) {
            return "CPU_AND_MEMORY_OVERLOAD";
        } else if (cpuOverloaded) {
            return "CPU_OVERLOAD";
        } else if (memoryOverloaded) {
            return "MEMORY_OVERLOAD";
        }

        return "LOAD_BALANCING";
    }

    /**
     * Builds LoadBalancingPlan from migration list.
     *
     * @param snapshot Current metrics snapshot
     * @param migrations List of migration actions
     * @param computationTime Computation time in milliseconds
     * @return LoadBalancingPlan instance
     */
    private LoadBalancingPlan buildPlan(MetricsSnapshot snapshot, List<MigrationAction> migrations,
                                       long computationTime) {
        return new LoadBalancingPlan.Builder()
            .timestamp(snapshot.getTimestamp())
            .algorithmName(POLICY_NAME)
            .migrations(migrations)
            .computationTime(computationTime)
            .build();
    }
}
