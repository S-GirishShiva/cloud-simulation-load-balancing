package com.cloudsimulation.core;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Memory-optimized extension of CloudSimIntegration that implements automatic VM cleanup
 * to prevent memory leaks in large-scale simulations.
 *
 * This class addresses the default CloudSim Plus behavior where completed VMs remain in memory
 * indefinitely, causing memory exhaustion in simulations with 500+ VMs.
 */
public class MemoryOptimizedCloudSim extends CloudSimIntegration {
    private static final Logger logger = LoggerFactory.getLogger(MemoryOptimizedCloudSim.class);

    private static final int GC_THRESHOLD = 100; // Suggest GC after cleaning 100+ VMs
    private boolean cleanupEnabled = false;
    private int totalVmsCleanedUp = 0;

    /**
     * Creates a new MemoryOptimizedCloudSim instance with automatic cleanup disabled by default.
     */
    public MemoryOptimizedCloudSim() {
        super();
        logger.info("MemoryOptimizedCloudSim created (cleanup disabled by default)");
    }

    /**
     * Enables or disables automatic VM cleanup after cloudlet completion.
     *
     * @param enabled true to enable automatic cleanup, false to disable
     */
    public void setCleanupEnabled(boolean enabled) {
        this.cleanupEnabled = enabled;
        logger.info("Automatic VM cleanup {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Checks if automatic cleanup is enabled.
     *
     * @return true if cleanup is enabled, false otherwise
     */
    public boolean isCleanupEnabled() {
        return cleanupEnabled;
    }

    /**
     * Overrides runFor() to perform automatic cleanup if enabled.
     *
     * @param duration Time duration to advance simulation (in simulation seconds)
     */
    @Override
    public void runFor(double duration) {
        super.runFor(duration);

        // Perform automatic cleanup after simulation tick if enabled
        if (cleanupEnabled && !isFinished()) {
            cleanupCompletedVms();
        }
    }

    /**
     * Cleans up VMs that have completed all their cloudlets.
     * Removes VMs from host, clears references, and suggests GC for large batches.
     */
    public void cleanupCompletedVms() {
        DatacenterBroker broker = getBroker();
        if (broker == null) {
            logger.warn("Cannot cleanup VMs: broker not initialized");
            return;
        }

        // Get all created VMs and check which ones are idle (no cloudlets running)
        List<Vm> createdVms = broker.getVmCreatedList();
        if (createdVms.isEmpty()) {
            logger.debug("No VMs to check for cleanup");
            return;
        }

        int cleanedCount = 0;
        // Create a copy to avoid concurrent modification
        List<Vm> vmsToCleanup = new java.util.ArrayList<>();

        for (Vm vm : createdVms) {
            // Check if VM is idle (no cloudlets running or waiting)
            if (vm.getCloudletScheduler().isEmpty()) {
                vmsToCleanup.add(vm);
            }
        }

        if (vmsToCleanup.isEmpty()) {
            logger.debug("No idle VMs to cleanup");
            return;
        }

        for (Vm vm : vmsToCleanup) {
            // Remove VM from host
            if (vm.getHost() != Host.NULL) {
                vm.getHost().destroyVm(vm);
                logger.debug("Destroyed VM {} from Host {}", vm.getId(), vm.getHost().getId());
            }

            // Clear VM references to allow garbage collection
            vm.setHost(Host.NULL);
            vm.setBroker(DatacenterBroker.NULL);

            // Remove from broker lists
            broker.getVmExecList().remove(vm);
            broker.getVmCreatedList().remove(vm);

            cleanedCount++;
        }

        totalVmsCleanedUp += cleanedCount;

        logger.info("Cleaned up {} VMs (total cleaned: {})", cleanedCount, totalVmsCleanedUp);

        // Suggest garbage collection for large batches
        if (cleanedCount >= GC_THRESHOLD) {
            logger.info("Suggesting GC after cleaning {} VMs", cleanedCount);
            System.gc();
        }
    }

    /**
     * Forces immediate cleanup of all completed VMs regardless of automatic cleanup setting.
     * Useful for manual memory management at specific points in simulation.
     */
    public void forceCleanup() {
        logger.info("Force cleanup requested");
        cleanupCompletedVms();
    }

    /**
     * Performs comprehensive cleanup of all VMs at end of simulation.
     * Should be called before termination for maximum memory reclamation.
     */
    public void cleanupAll() {
        logger.info("Cleaning up all VMs");
        cleanupCompletedVms();

        // Force GC to ensure memory is reclaimed
        logger.info("Forcing garbage collection");
        System.gc();
    }

    /**
     * Gets memory statistics for monitoring cleanup effectiveness.
     *
     * @return MemoryStats object with current memory usage information
     */
    public MemoryStats getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        long maxMemory = runtime.maxMemory();

        return new MemoryStats(totalMemory, freeMemory, usedMemory, maxMemory, totalVmsCleanedUp);
    }

    /**
     * Overrides cleanup hook to perform VM cleanup before simulation termination.
     */
    @Override
    protected void cleanupHook() {
        logger.info("Cleanup hook called - performing final VM cleanup");
        cleanupAll();
    }

    /**
     * Gets the total number of VMs cleaned up during this simulation.
     *
     * @return Total VM cleanup count
     */
    public int getTotalVmsCleanedUp() {
        return totalVmsCleanedUp;
    }

    /**
     * Memory statistics container.
     */
    public static class MemoryStats {
        private final long totalMemoryBytes;
        private final long freeMemoryBytes;
        private final long usedMemoryBytes;
        private final long maxMemoryBytes;
        private final int vmsCleanedUp;

        public MemoryStats(long total, long free, long used, long max, int vmsCleanedUp) {
            this.totalMemoryBytes = total;
            this.freeMemoryBytes = free;
            this.usedMemoryBytes = used;
            this.maxMemoryBytes = max;
            this.vmsCleanedUp = vmsCleanedUp;
        }

        public long getTotalMemoryMB() {
            return totalMemoryBytes / (1024 * 1024);
        }

        public long getFreeMemoryMB() {
            return freeMemoryBytes / (1024 * 1024);
        }

        public long getUsedMemoryMB() {
            return usedMemoryBytes / (1024 * 1024);
        }

        public long getMaxMemoryMB() {
            return maxMemoryBytes / (1024 * 1024);
        }

        public int getVmsCleanedUp() {
            return vmsCleanedUp;
        }

        @Override
        public String toString() {
            return String.format("Memory: %d/%d MB used (max: %d MB), VMs cleaned: %d",
                    getUsedMemoryMB(), getTotalMemoryMB(), getMaxMemoryMB(), vmsCleanedUp);
        }
    }
}
