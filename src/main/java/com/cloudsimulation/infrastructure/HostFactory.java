package com.cloudsimulation.infrastructure;

import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.vm.VmScheduler;
import org.cloudsimplus.schedulers.vm.VmSchedulerTimeShared;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating CloudSim Plus Host instances.
 * Follows the Factory pattern for host instantiation with proper VM scheduler configuration.
 */
public class HostFactory {
    private static final Logger logger = LoggerFactory.getLogger(HostFactory.class);

    // Default host configuration
    private static final long DEFAULT_MIPS = 10000;
    private static final long DEFAULT_RAM = 32768; // 32 GB in MB
    private static final long DEFAULT_STORAGE = 1000000; // 1 TB in MB
    private static final long DEFAULT_BW = 10000; // 10 Gbps in Mbps

    /**
     * Creates a new HostFactory instance.
     */
    public HostFactory() {
        logger.info("HostFactory initialized");
    }

    /**
     * Creates a host with default specifications.
     * Uses VmSchedulerTimeShared which implements basic First Fit placement strategy.
     *
     * @param numberOfPes Number of processing elements (CPU cores) for the host
     * @return Configured Host instance
     */
    public Host createHost(int numberOfPes) {
        return createHost(numberOfPes, DEFAULT_MIPS, DEFAULT_RAM, DEFAULT_STORAGE, DEFAULT_BW);
    }

    /**
     * Creates a host with specified resource specifications.
     * Uses VmSchedulerTimeShared which implements basic First Fit placement strategy.
     *
     * @param numberOfPes Number of processing elements (CPU cores) for the host
     * @param mipsPerPe MIPS (Million Instructions Per Second) capacity per PE
     * @param ram RAM capacity in MB
     * @param storage Storage capacity in MB
     * @param bw Bandwidth capacity in Mbps
     * @return Configured Host instance
     * @throws IllegalArgumentException if any parameter is non-positive
     */
    public Host createHost(int numberOfPes, long mipsPerPe, long ram, long storage, long bw) {
        if (numberOfPes <= 0 || mipsPerPe <= 0 || ram <= 0 || storage <= 0 || bw <= 0) {
            throw new IllegalArgumentException("All host resource parameters must be positive");
        }

        // Create processing elements (CPU cores) for the host
        List<Pe> peList = new ArrayList<>();
        for (int i = 0; i < numberOfPes; i++) {
            peList.add(new PeSimple(mipsPerPe));
        }

        // Create host with VmSchedulerTimeShared (First Fit strategy)
        VmScheduler vmScheduler = new VmSchedulerTimeShared();
        Host host = new HostSimple(ram, bw, storage, peList);
        host.setVmScheduler(vmScheduler);

        logger.info("Host {} created: {} PEs @ {} MIPS, {} MB RAM, {} MB Storage, {} Mbps BW",
                    host.getId(), numberOfPes, mipsPerPe, ram, storage, bw);
        return host;
    }

    /**
     * Creates a list of hosts with default specifications.
     *
     * @param count Number of hosts to create
     * @param numberOfPesPerHost Number of PEs per host
     * @return List of configured Host instances
     * @throws IllegalArgumentException if count is non-positive
     */
    public List<Host> createHosts(int count, int numberOfPesPerHost) {
        if (count <= 0) {
            throw new IllegalArgumentException("Host count must be positive");
        }

        List<Host> hosts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            hosts.add(createHost(numberOfPesPerHost));
        }

        logger.info("Created {} hosts with {} PEs each", count, numberOfPesPerHost);
        return hosts;
    }

    /**
     * Creates a list of hosts with specified resource specifications.
     *
     * @param count Number of hosts to create
     * @param numberOfPesPerHost Number of PEs per host
     * @param mipsPerPe MIPS capacity per PE
     * @param ram RAM capacity in MB
     * @param storage Storage capacity in MB
     * @param bw Bandwidth capacity in Mbps
     * @return List of configured Host instances
     * @throws IllegalArgumentException if count is non-positive
     */
    public List<Host> createHosts(int count, int numberOfPesPerHost, long mipsPerPe,
                                  long ram, long storage, long bw) {
        if (count <= 0) {
            throw new IllegalArgumentException("Host count must be positive");
        }

        List<Host> hosts = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            hosts.add(createHost(numberOfPesPerHost, mipsPerPe, ram, storage, bw));
        }

        logger.info("Created {} hosts with custom specs: {} PEs @ {} MIPS each",
                    count, numberOfPesPerHost, mipsPerPe);
        return hosts;
    }
}
