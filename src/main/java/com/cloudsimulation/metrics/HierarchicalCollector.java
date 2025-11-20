package com.cloudsimulation.metrics;

import com.cloudsimulation.core.CloudSimIntegration;
import com.cloudsimulation.infrastructure.Federation;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implements efficient metrics collection with lazy aggregation and hierarchical traversal.
 * Uses 5-tick caching to minimize performance overhead.
 */
public class HierarchicalCollector implements MetricsCollector {
    private static final Logger logger = LoggerFactory.getLogger(HierarchicalCollector.class);
    private static final double OVERLOAD_THRESHOLD = 0.90;
    private static final double UNDERLOAD_THRESHOLD = 0.20;

    private final MetricsCache cache;
    private final AtomicInteger migrationCount;
    private double lastAverageUtilization;

    /**
     * Creates a new HierarchicalCollector with empty cache.
     */
    public HierarchicalCollector() {
        this.cache = new MetricsCache();
        this.migrationCount = new AtomicInteger(0);
        this.lastAverageUtilization = 0.0;
        logger.info("HierarchicalCollector initialized");
    }

    /**
     * Collects metrics from Federation hierarchy with lazy caching.
     *
     * @param cloudSim CloudSim integration instance to collect metrics from
     * @return MetricsSnapshot containing current system state
     */
    @Override
    public MetricsSnapshot collect(CloudSimIntegration cloudSim) {
        if (cloudSim == null) {
            throw new IllegalArgumentException("CloudSimIntegration cannot be null");
        }

        double currentTime = cloudSim.getElapsedTime();

        // Check cache first (lazy evaluation)
        MetricsSnapshot cached = cache.get(currentTime);
        if (cached != null) {
            logger.debug("Returning cached metrics for time {}", currentTime);
            return cached;
        }

        // Cache miss - perform hierarchical aggregation
        logger.debug("Cache miss - collecting metrics at time {}", currentTime);

        List<Federation> federations = cloudSim.getFederations();

        // Aggregation variables
        double totalCpuUtilization = 0.0;
        double totalMemoryUtilization = 0.0;
        int hostCount = 0;
        int overloadedVms = 0;
        int underloadedVms = 0;
        int activeVms = 0;

        // Hierarchical traversal: Federation → Datacenters → Hosts → VMs
        for (Federation federation : federations) {
            for (Datacenter datacenter : federation.getDatacenters()) {
                for (Host host : datacenter.getHostList()) {
                    hostCount++;

                    // Aggregate host CPU utilization
                    double hostCpuUtilization = host.getCpuPercentUtilization();
                    totalCpuUtilization += hostCpuUtilization;

                    // Aggregate host memory utilization
                    double hostMemoryUtilization = calculateHostMemoryUtilization(host);
                    totalMemoryUtilization += hostMemoryUtilization;

                    // Count VMs and check thresholds
                    for (Vm vm : host.getVmList()) {
                        activeVms++;

                        double vmCpuUtilization = vm.getCpuPercentUtilization();
                        if (vmCpuUtilization > OVERLOAD_THRESHOLD) {
                            overloadedVms++;
                        } else if (vmCpuUtilization < UNDERLOAD_THRESHOLD) {
                            underloadedVms++;
                        }
                    }
                }
            }
        }

        // Calculate averages
        double avgCpuUtilization = hostCount > 0 ? totalCpuUtilization / hostCount : 0.0;
        double avgMemoryUtilization = hostCount > 0 ? totalMemoryUtilization / hostCount : 0.0;

        // Store for getAverageUtilization()
        lastAverageUtilization = avgCpuUtilization;

        // Create snapshot (powerConsumption set to 0.0 for future use)
        MetricsSnapshot snapshot = new MetricsSnapshot(
            currentTime,
            avgCpuUtilization,
            avgMemoryUtilization,
            overloadedVms,
            underloadedVms,
            migrationCount.get(),
            activeVms,
            0.0 // powerConsumption - future use
        );

        // Cache the snapshot
        cache.put(currentTime, snapshot);

        logger.debug("Collected metrics: {}", snapshot);
        return snapshot;
    }

    /**
     * Calculates memory utilization for a host.
     *
     * @param host Host to calculate memory utilization for
     * @return Memory utilization ratio (0-1 range)
     */
    private double calculateHostMemoryUtilization(Host host) {
        long totalRam = host.getRam().getCapacity();
        long allocatedRam = host.getRam().getAllocatedResource();

        if (totalRam == 0) {
            return 0.0;
        }

        return (double) allocatedRam / totalRam;
    }

    /**
     * Records a VM migration event.
     * NOTE: Migration tracking will be integrated in future stories (Story 2.x).
     *
     * @param migration VM migration event (currently unused)
     */
    public void recordMigration(Object migration) {
        migrationCount.incrementAndGet();
        logger.debug("Migration recorded. Total migrations: {}", migrationCount.get());
    }

    /**
     * Gets the total number of VM migrations that have occurred.
     *
     * @return Cumulative migration count
     */
    @Override
    public int getTotalMigrations() {
        return migrationCount.get();
    }

    /**
     * Gets the overall average CPU utilization from the last collection.
     *
     * @return Average CPU utilization across all hosts
     */
    @Override
    public double getAverageUtilization() {
        return lastAverageUtilization;
    }

    /**
     * Invalidates the metrics cache, forcing re-collection on next collect() call.
     */
    public void invalidateCache() {
        cache.invalidate();
        logger.debug("Metrics cache invalidated");
    }
}
