package com.cloudsimulation.infrastructure;

import org.cloudsimplus.allocationpolicies.VmAllocationPolicySimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Factory class for creating CloudSim Plus Datacenter instances.
 * Follows the Factory pattern for datacenter instantiation with proper configuration.
 */
public class DatacenterFactory {
    private static final Logger logger = LoggerFactory.getLogger(DatacenterFactory.class);

    // Default datacenter characteristics
    private static final String DEFAULT_ARCHITECTURE = "x86";
    private static final String DEFAULT_OS = "Linux";
    private static final String DEFAULT_VMM = "Xen";
    private static final double DEFAULT_COST_PER_SECOND = 3.0;
    private static final double DEFAULT_COST_PER_MEM = 0.05;
    private static final double DEFAULT_COST_PER_STORAGE = 0.001;
    private static final double DEFAULT_COST_PER_BW = 0.0;

    private final CloudSimPlus simulation;

    /**
     * Creates a new DatacenterFactory instance.
     *
     * @param simulation CloudSim simulation instance for datacenter registration
     */
    public DatacenterFactory(CloudSimPlus simulation) {
        this.simulation = simulation;
        logger.info("DatacenterFactory initialized");
    }

    /**
     * Creates a datacenter with the specified host list.
     *
     * @param hostList List of hosts to be assigned to this datacenter
     * @return Configured Datacenter instance
     * @throws IllegalArgumentException if hostList is null or empty
     */
    public Datacenter createDatacenter(List<Host> hostList) {
        if (hostList == null || hostList.isEmpty()) {
            throw new IllegalArgumentException("Host list cannot be null or empty");
        }

        Datacenter datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());

        // Configure datacenter characteristics
        datacenter.getCharacteristics()
                .setArchitecture(DEFAULT_ARCHITECTURE)
                .setOs(DEFAULT_OS)
                .setVmm(DEFAULT_VMM)
                .setCostPerSecond(DEFAULT_COST_PER_SECOND)
                .setCostPerMem(DEFAULT_COST_PER_MEM)
                .setCostPerStorage(DEFAULT_COST_PER_STORAGE)
                .setCostPerBw(DEFAULT_COST_PER_BW);

        logger.info("Datacenter {} created with {} hosts", datacenter.getId(), hostList.size());
        return datacenter;
    }

    /**
     * Creates a datacenter with custom characteristics.
     *
     * @param hostList List of hosts to be assigned to this datacenter
     * @param architecture Architecture type (e.g., "x86", "ARM")
     * @param os Operating system (e.g., "Linux", "Windows")
     * @param vmm Virtual Machine Monitor (e.g., "Xen", "KVM")
     * @param costPerSecond Cost per second of CPU usage
     * @return Configured Datacenter instance
     * @throws IllegalArgumentException if hostList is null or empty
     */
    public Datacenter createDatacenter(List<Host> hostList, String architecture,
                                       String os, String vmm, double costPerSecond) {
        if (hostList == null || hostList.isEmpty()) {
            throw new IllegalArgumentException("Host list cannot be null or empty");
        }

        Datacenter datacenter = new DatacenterSimple(simulation, hostList, new VmAllocationPolicySimple());

        // Configure datacenter with custom characteristics
        datacenter.getCharacteristics()
                .setArchitecture(architecture)
                .setOs(os)
                .setVmm(vmm)
                .setCostPerSecond(costPerSecond)
                .setCostPerMem(DEFAULT_COST_PER_MEM)
                .setCostPerStorage(DEFAULT_COST_PER_STORAGE)
                .setCostPerBw(DEFAULT_COST_PER_BW);

        logger.info("Datacenter {} created with custom config: {} hosts, {} OS, {} VMM",
                    datacenter.getId(), hostList.size(), os, vmm);
        return datacenter;
    }
}
