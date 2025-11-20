package com.cloudsimulation.infrastructure;

import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Federation manages multiple Datacenter instances as a unified federated cloud system.
 * This is a custom abstraction layer built on top of CloudSim Plus components.
 *
 * Thread-safe implementation using ConcurrentHashMap for datacenter management.
 */
public class Federation {
    private static final Logger logger = LoggerFactory.getLogger(Federation.class);

    private final Map<Integer, Datacenter> datacenters;
    private final String federationId;

    /**
     * Creates a new Federation instance with a unique identifier.
     *
     * @param federationId Unique identifier for this federation
     */
    public Federation(String federationId) {
        this.federationId = federationId;
        this.datacenters = new ConcurrentHashMap<>();
        logger.info("Federation {} initialized", federationId);
    }

    /**
     * Adds a datacenter to this federation.
     *
     * @param id Unique identifier for the datacenter
     * @param datacenter CloudSim Plus Datacenter instance
     * @return true if datacenter was added, false if ID already exists
     */
    public boolean addDatacenter(int id, Datacenter datacenter) {
        if (datacenters.containsKey(id)) {
            logger.warn("Datacenter with ID {} already exists in federation {}", id, federationId);
            return false;
        }
        datacenters.put(id, datacenter);
        logger.info("Datacenter {} added to federation {}", id, federationId);
        return true;
    }

    /**
     * Removes a datacenter from this federation.
     *
     * @param id Unique identifier of the datacenter to remove
     * @return true if datacenter was removed, false if ID not found
     */
    public boolean removeDatacenter(int id) {
        Datacenter removed = datacenters.remove(id);
        if (removed != null) {
            logger.info("Datacenter {} removed from federation {}", id, federationId);
            return true;
        }
        logger.warn("Datacenter {} not found in federation {}", id, federationId);
        return false;
    }

    /**
     * Gets a datacenter by its unique identifier.
     *
     * @param id Unique identifier of the datacenter
     * @return Datacenter instance or null if not found
     */
    public Datacenter getDatacenter(int id) {
        return datacenters.get(id);
    }

    /**
     * Returns all managed datacenters in this federation.
     *
     * @return Unmodifiable list of all datacenters
     */
    public List<Datacenter> getDatacenters() {
        return new ArrayList<>(datacenters.values());
    }

    /**
     * Calculates total capacity across all datacenters in the federation.
     * Total capacity is the sum of all host MIPS across all datacenters.
     *
     * @return Total MIPS capacity across all datacenters
     */
    public long getTotalCapacity() {
        long totalCapacity = 0;
        for (Datacenter datacenter : datacenters.values()) {
            for (Host host : datacenter.getHostList()) {
                totalCapacity += host.getTotalMipsCapacity();
            }
        }
        logger.debug("Total capacity for federation {}: {} MIPS", federationId, totalCapacity);
        return totalCapacity;
    }

    /**
     * Calculates current utilization across all datacenters in the federation.
     * Utilization is the percentage of used MIPS capacity relative to total capacity.
     *
     * @return Utilization percentage (0.0 to 1.0), or 0.0 if total capacity is zero
     */
    public double getUtilization() {
        long totalCapacity = 0;
        long usedCapacity = 0;

        for (Datacenter datacenter : datacenters.values()) {
            for (Host host : datacenter.getHostList()) {
                totalCapacity += host.getTotalMipsCapacity();
                usedCapacity += host.getTotalAllocatedMips();
            }
        }

        if (totalCapacity == 0) {
            logger.warn("Total capacity is zero for federation {}", federationId);
            return 0.0;
        }

        double utilization = (double) usedCapacity / totalCapacity;
        logger.debug("Utilization for federation {}: {:.2f}%", federationId, utilization * 100);
        return utilization;
    }

    /**
     * Gets the federation identifier.
     *
     * @return Federation unique identifier
     */
    public String getFederationId() {
        return federationId;
    }

    /**
     * Gets the number of datacenters in this federation.
     *
     * @return Number of datacenters
     */
    public int getDatacenterCount() {
        return datacenters.size();
    }

    /**
     * Interface for inter-datacenter communication.
     * Initially implemented as placeholder methods for future multi-region scenarios.
     */
    public interface InterDatacenterCommunication {
        /**
         * Sends data from one datacenter to another within the federation.
         *
         * @param sourceId Source datacenter ID
         * @param destinationId Destination datacenter ID
         * @param data Data to transfer
         * @return true if transfer initiated successfully
         */
        boolean transferData(int sourceId, int destinationId, Object data);

        /**
         * Establishes a communication channel between two datacenters.
         *
         * @param datacenter1Id First datacenter ID
         * @param datacenter2Id Second datacenter ID
         * @return true if channel established
         */
        boolean establishChannel(int datacenter1Id, int datacenter2Id);

        /**
         * Gets the network latency between two datacenters.
         *
         * @param datacenter1Id First datacenter ID
         * @param datacenter2Id Second datacenter ID
         * @return Latency in milliseconds
         */
        double getLatency(int datacenter1Id, int datacenter2Id);
    }
}
