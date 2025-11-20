package com.cloudsimulation.core;

import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.models.LoadBalancingPlan;
import com.cloudsimulation.models.MigrationAction;
import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * CloudSimIntegration wraps CloudSim Plus simulation engine to provide
 * simplified interface for simulation lifecycle management.
 *
 * Manages simulation initialization, event loop execution, and resource cleanup.
 */
public class CloudSimIntegration {
    private static final Logger logger = LoggerFactory.getLogger(CloudSimIntegration.class);

    private CloudSimPlus simulation;
    private DatacenterBroker broker;
    private SimulationState state;
    private List<Federation> federations;
    private double simulationStartTime = -1.0;  // Tracks absolute clock time when simulation started

    /**
     * Simulation execution states.
     */
    private enum SimulationState {
        NOT_STARTED,
        RUNNING,
        FINISHED,
        FAILED
    }

    /**
     * Creates a new CloudSimIntegration instance.
     * Simulation must be initialized before use.
     */
    public CloudSimIntegration() {
        this.state = SimulationState.NOT_STARTED;
        logger.info("CloudSimIntegration instance created");
    }

    /**
     * Initializes CloudSim Plus simulation with configurable clock tick rate.
     *
     * @param simulation CloudSimPlus simulation instance (from FederationBuilder)
     * @param federations List of Federation instances to register with simulation
     * @param clockTickRate Clock tick rate in seconds (granularity of simulation time)
     */
    public void initialize(CloudSimPlus simulation, List<Federation> federations, double clockTickRate) {
        if (simulation == null) {
            throw new IllegalArgumentException("Simulation instance is required");
        }
        if (federations == null || federations.isEmpty()) {
            throw new IllegalArgumentException("At least one federation is required");
        }
        if (clockTickRate <= 0) {
            throw new IllegalArgumentException("Clock tick rate must be positive");
        }

        logger.info("Initializing simulation with {} federation(s), clock tick rate: {}",
                    federations.size(), clockTickRate);

        // Use provided CloudSim Plus simulation instance
        this.simulation = simulation;

        // Store federations for metrics collection
        this.federations = federations;

        // Create and link DatacenterBroker
        this.broker = new DatacenterBrokerSimple(simulation);

        // Datacenters are already registered with simulation when created via DatacenterFactory
        // Just log the federation setup
        for (Federation federation : federations) {
            logger.info("Federation {} has {} datacenters",
                        federation.getFederationId(), federation.getDatacenterCount());

            // Verify datacenters are registered
            for (Datacenter datacenter : federation.getDatacenters()) {
                logger.debug("Datacenter {} registered with {} hosts",
                            datacenter.getId(), datacenter.getHostList().size());
            }
        }

        logger.info("Simulation initialized with broker ID: {}", broker.getId());
    }

    /**
     * Initializes CloudSim Plus simulation with default clock tick rate (1.0 second).
     *
     * @param simulation CloudSimPlus simulation instance (from FederationBuilder)
     * @param federations List of Federation instances to register with simulation
     */
    public void initialize(CloudSimPlus simulation, List<Federation> federations) {
        initialize(simulation, federations, 1.0);
    }

    /**
     * Submits VMs to DatacenterBroker for automatic host assignment using First Fit strategy.
     * This completes the deferred VM-to-host assignment from Story 1.2.
     *
     * @param vms List of VMs to submit for placement
     * @return List of successfully submitted VMs
     */
    public List<Vm> submitVms(List<Vm> vms) {
        if (broker == null) {
            throw new IllegalStateException("Simulation not initialized. Call initialize() first.");
        }
        if (vms == null || vms.isEmpty()) {
            logger.warn("No VMs to submit");
            return List.of();
        }

        logger.info("Submitting {} VMs to broker for host assignment", vms.size());
        broker.submitVmList(vms);

        logger.info("Successfully submitted {} VMs to broker {}", vms.size(), broker.getId());
        return vms;
    }

    /**
     * Submits cloudlets to DatacenterBroker for execution on VMs.
     * Broker automatically assigns cloudlets to VMs based on availability.
     *
     * @param cloudlets List of cloudlets to submit
     */
    public void submitCloudlets(List<Cloudlet> cloudlets) {
        if (broker == null) {
            throw new IllegalStateException("Simulation not initialized. Call initialize() first.");
        }
        if (cloudlets == null || cloudlets.isEmpty()) {
            logger.warn("No cloudlets to submit");
            return;
        }

        logger.info("Submitting {} cloudlets to broker", cloudlets.size());
        broker.submitCloudletList(cloudlets);

        logger.info("Successfully submitted {} cloudlets to broker {}", cloudlets.size(), broker.getId());
    }

    /**
     * Starts the simulation event loop.
     * Must be called after VMs and cloudlets are submitted.
     */
    public void start() {
        if (simulation == null) {
            throw new IllegalStateException("Simulation not initialized. Call initialize() first.");
        }
        if (state != SimulationState.NOT_STARTED) {
            throw new IllegalStateException("Simulation already started");
        }

        logger.info("Starting simulation");

        try {
            // Set state to RUNNING before calling simulation.start() so getElapsedTime() works
            // simulation.start() is synchronous and runs entire simulation before returning
            state = SimulationState.RUNNING;

            // Start time will be captured on first clock tick (see getElapsedTime())
            // CloudSim Plus clock may not be initialized until events start processing
            simulation.start();
            logger.info("Simulation completed");
        } catch (Exception e) {
            state = SimulationState.FAILED;
            logger.error("Simulation failed to start", e);
            throw new RuntimeException("Failed to start simulation", e);
        }
    }

    /**
     * Advances simulation by specified duration.
     *
     * @param duration Time duration to advance simulation (in simulation seconds)
     */
    public void runFor(double duration) {
        if (simulation == null) {
            throw new IllegalStateException("Simulation not initialized");
        }
        if (state != SimulationState.RUNNING) {
            throw new IllegalStateException("Simulation not running. Call start() first.");
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration must be positive");
        }

        try {
            double targetTime = simulation.clock() + duration;
            logger.debug("Running simulation from {} to {}", simulation.clock(), targetTime);

            // CloudSim Plus doesn't have runFor(), so we use runFor equivalent
            // by processing events until target time or no more events
            while (simulation.isRunning() && simulation.clock() < targetTime) {
                simulation.runFor(duration);
                break; // runFor handles duration internally
            }

            if (!simulation.isRunning()) {
                state = SimulationState.FINISHED;
                logger.info("Simulation finished at time {}", simulation.clock());
            }
        } catch (Exception e) {
            state = SimulationState.FAILED;
            logger.error("Simulation runtime error", e);
            throw new RuntimeException("Simulation execution failed", e);
        }
    }

    /**
     * Checks if simulation has finished processing all events.
     *
     * @return true if simulation completed, false otherwise
     */
    public boolean isFinished() {
        if (simulation == null || state == SimulationState.NOT_STARTED) {
            return false;
        }
        return state == SimulationState.FINISHED || !simulation.isRunning();
    }

    /**
     * Gets the current simulation time.
     *
     * @return Current simulation clock time in seconds
     */
    public double clock() {
        if (simulation == null) {
            return 0.0;
        }
        return simulation.clock();
    }

    /**
     * Gets elapsed simulation time since start() was called.
     * This provides time relative to simulation start (0.0 at start),
     * rather than the absolute CloudSim Plus clock value.
     *
     * On first call during simulation, captures the start time (lazy initialization).
     * This is necessary because CloudSim Plus clock is not initialized until events start processing.
     *
     * @return Elapsed time in seconds since simulation started, or 0.0 if not yet started
     */
    public double getElapsedTime() {
        if (simulation == null) {
            return 0.0;  // Simulation not initialized
        }

        // Lazy initialization: capture start time on first call during simulation
        if (simulationStartTime < 0 && state == SimulationState.RUNNING) {
            simulationStartTime = simulation.clock();
            System.out.printf("!!! CAPTURED START TIME: %.2f !!!%n", simulationStartTime);
            logger.info("Captured simulation start time: {}", simulationStartTime);
        }

        if (simulationStartTime < 0) {
            return 0.0;  // Start time not yet captured
        }

        return simulation.clock() - simulationStartTime;
    }

    /**
     * Gets the Simulation instance for advanced access.
     *
     * @return CloudSimPlus simulation instance
     */
    public CloudSimPlus getSimulation() {
        return simulation;
    }

    /**
     * Gets the DatacenterBroker instance.
     *
     * @return DatacenterBroker instance
     */
    public DatacenterBroker getBroker() {
        return broker;
    }

    /**
     * Gets the current simulation state.
     *
     * @return Current simulation state
     */
    public String getState() {
        return state.toString();
    }

    /**
     * Gets the list of federations registered with this simulation.
     * Used by metrics collection for hierarchical traversal.
     *
     * @return List of Federation instances
     * @throws IllegalStateException if federations not initialized
     */
    public List<Federation> getFederations() {
        if (federations == null) {
            throw new IllegalStateException("Federations not initialized. Call initialize() first.");
        }
        return federations;
    }

    /**
     * Executes a load balancing plan by performing VM migrations.
     * Translates high-level migration actions to CloudSim Plus API calls.
     *
     * <p><b>Migration Process:</b></p>
     * <ol>
     *   <li>Validates VM and host existence</li>
     *   <li>Checks target host capacity</li>
     *   <li>Executes migration via CloudSim Plus API</li>
     *   <li>Tracks successful migrations</li>
     *   <li>Logs all migration attempts and results</li>
     * </ol>
     *
     * @param plan The load balancing plan to execute
     * @return Number of successful migrations
     * @throws IllegalArgumentException if plan is null
     * @throws IllegalStateException if simulation not initialized
     */
    public int executeMigrationPlan(LoadBalancingPlan plan) {
        if (plan == null) {
            throw new IllegalArgumentException("LoadBalancingPlan cannot be null");
        }
        if (simulation == null) {
            throw new IllegalStateException("Simulation not initialized. Call initialize() first.");
        }

        logger.info("Executing migration plan: {}", plan);

        int successCount = 0;
        int failureCount = 0;

        for (MigrationAction action : plan.getMigrations()) {
            try {
                // Step 1: Find the VM
                Optional<Vm> vmOpt = findVmById(action.getVmId());
                if (vmOpt.isEmpty()) {
                    logger.warn("Migration failed: VM {} not found", action.getVmId());
                    failureCount++;
                    continue;
                }
                Vm vm = vmOpt.get();

                // Step 2: Find target host
                Optional<Host> targetHostOpt = findHostById(action.getTargetHostId());
                if (targetHostOpt.isEmpty()) {
                    logger.warn("Migration failed: Target host {} not found", action.getTargetHostId());
                    failureCount++;
                    continue;
                }
                Host targetHost = targetHostOpt.get();

                // Step 3: Validate current host matches source
                if (vm.getHost().getId() != action.getSourceHostId()) {
                    logger.warn(
                        "Migration warning: VM {} current host {} doesn't match expected source host {}",
                        action.getVmId(), vm.getHost().getId(), action.getSourceHostId()
                    );
                }

                // Step 4: Check target host capacity
                if (!hasCapacity(targetHost, vm)) {
                    logger.warn(
                        "Migration failed: Target host {} insufficient capacity for VM {} (requires {} MIPS, {} MB RAM)",
                        action.getTargetHostId(), action.getVmId(),
                        vm.getMips(), vm.getRam().getCapacity()
                    );
                    failureCount++;
                    continue;
                }

                // Step 5: Execute migration
                Host sourceHost = vm.getHost();
                vm.setHost(targetHost);

                logger.info(
                    "Migration successful: VM {} moved from host {} to host {} (reason: {})",
                    action.getVmId(), sourceHost.getId(), targetHost.getId(), action.getMigrationReason()
                );

                successCount++;

            } catch (Exception e) {
                logger.error("Migration failed for VM {}: {}", action.getVmId(), e.getMessage(), e);
                failureCount++;
            }
        }

        logger.info(
            "Migration plan execution complete: {} successful, {} failed out of {} total",
            successCount, failureCount, plan.getMigrationCount()
        );

        return successCount;
    }

    /**
     * Finds a VM by its ID across all federations.
     *
     * @param vmId VM identifier
     * @return Optional containing VM if found, empty otherwise
     */
    private Optional<Vm> findVmById(int vmId) {
        if (broker == null) {
            return Optional.empty();
        }

        return broker.getVmCreatedList().stream()
            .filter(vm -> vm.getId() == vmId)
            .findFirst();
    }

    /**
     * Finds a host by its ID across all federations.
     *
     * @param hostId Host identifier
     * @return Optional containing Host if found, empty otherwise
     */
    private Optional<Host> findHostById(int hostId) {
        if (federations == null) {
            return Optional.empty();
        }

        for (Federation federation : federations) {
            for (Datacenter datacenter : federation.getDatacenters()) {
                Optional<Host> hostOpt = datacenter.getHostList().stream()
                    .filter(host -> host.getId() == hostId)
                    .findFirst();

                if (hostOpt.isPresent()) {
                    return hostOpt;
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Checks if a host has sufficient capacity for a VM.
     *
     * @param host Target host
     * @param vm VM to migrate
     * @return true if host has capacity, false otherwise
     */
    private boolean hasCapacity(Host host, Vm vm) {
        // Check available MIPS
        double availableMips = host.getTotalMipsCapacity() - host.getTotalAllocatedMips();
        if (availableMips < vm.getTotalMipsCapacity()) {
            return false;
        }

        // Check available RAM
        long availableRam = host.getRam().getAvailableResource();
        if (availableRam < vm.getRam().getCapacity()) {
            return false;
        }

        // Check available bandwidth
        long availableBw = host.getBw().getAvailableResource();
        if (availableBw < vm.getBw().getCapacity()) {
            return false;
        }

        // Check available storage
        long availableStorage = host.getStorage().getAvailableResource();
        if (availableStorage < vm.getStorage().getCapacity()) {
            return false;
        }

        return true;
    }

    /**
     * Terminates the simulation and cleans up resources.
     * Should be called in finally block to ensure proper cleanup.
     */
    public void terminate() {
        if (simulation != null) {
            logger.info("Terminating simulation at time {}", simulation.clock());
            try {
                // Call cleanup hook before termination (Story 2.2)
                cleanupHook();

                simulation.terminate();
                state = SimulationState.FINISHED;
                logger.info("Simulation terminated successfully");
            } catch (Exception e) {
                logger.error("Error during simulation termination", e);
            }
        }
    }

    /**
     * Cleanup hook called before simulation termination.
     * Subclasses can override this method to perform custom cleanup operations.
     * Default implementation does nothing to maintain backward compatibility.
     */
    protected void cleanupHook() {
        // Default: no-op
        // Subclasses like MemoryOptimizedCloudSim override this for VM cleanup
    }
}
