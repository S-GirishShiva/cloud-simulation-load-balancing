package com.cloudsimulation.infrastructure;

import org.cloudsimplus.schedulers.cloudlet.CloudletScheduler;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerTimeShared;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for creating CloudSim Plus VM instances.
 * Follows the Factory pattern for VM instantiation with proper cloudlet scheduler configuration.
 */
public class VmFactory {
    private static final Logger logger = LoggerFactory.getLogger(VmFactory.class);

    // Default VM configuration
    private static final long DEFAULT_MIPS = 1000;
    private static final long DEFAULT_RAM = 2048; // 2 GB in MB
    private static final long DEFAULT_STORAGE = 10000; // 10 GB in MB
    private static final long DEFAULT_BW = 1000; // 1 Gbps in Mbps
    private static final int DEFAULT_PES = 1; // Number of CPU cores

    /**
     * Creates a new VmFactory instance.
     */
    public VmFactory() {
        logger.info("VmFactory initialized");
    }

    /**
     * Creates a VM with default specifications.
     * Uses CloudletSchedulerTimeShared for cloudlet execution.
     *
     * @return Configured VM instance
     */
    public Vm createVm() {
        return createVm(DEFAULT_PES, DEFAULT_MIPS, DEFAULT_RAM, DEFAULT_STORAGE, DEFAULT_BW);
    }

    /**
     * Creates a VM with specified resource specifications.
     * Uses CloudletSchedulerTimeShared for cloudlet execution.
     *
     * @param numberOfPes Number of processing elements (vCPUs) for the VM
     * @param mips MIPS capacity for the VM
     * @param ram RAM capacity in MB
     * @param storage Storage capacity in MB
     * @param bw Bandwidth capacity in Mbps
     * @return Configured VM instance
     * @throws IllegalArgumentException if any parameter is non-positive
     */
    public Vm createVm(int numberOfPes, long mips, long ram, long storage, long bw) {
        if (numberOfPes <= 0 || mips <= 0 || ram <= 0 || storage <= 0 || bw <= 0) {
            throw new IllegalArgumentException("All VM resource parameters must be positive");
        }

        // Create VM with CloudletSchedulerTimeShared
        CloudletScheduler cloudletScheduler = new CloudletSchedulerTimeShared();
        Vm vm = new VmSimple(mips, numberOfPes);
        vm.setRam(ram)
          .setBw(bw)
          .setSize(storage)
          .setCloudletScheduler(cloudletScheduler);

        logger.info("VM {} created: {} PEs @ {} MIPS, {} MB RAM, {} MB Storage, {} Mbps BW",
                    vm.getId(), numberOfPes, mips, ram, storage, bw);
        return vm;
    }

    /**
     * Creates a VM with custom cloudlet scheduler.
     *
     * @param numberOfPes Number of processing elements (vCPUs) for the VM
     * @param mips MIPS capacity for the VM
     * @param ram RAM capacity in MB
     * @param storage Storage capacity in MB
     * @param bw Bandwidth capacity in Mbps
     * @param cloudletScheduler Custom cloudlet scheduler
     * @return Configured VM instance
     * @throws IllegalArgumentException if any parameter is invalid
     */
    public Vm createVm(int numberOfPes, long mips, long ram, long storage, long bw,
                       CloudletScheduler cloudletScheduler) {
        if (numberOfPes <= 0 || mips <= 0 || ram <= 0 || storage <= 0 || bw <= 0) {
            throw new IllegalArgumentException("All VM resource parameters must be positive");
        }
        if (cloudletScheduler == null) {
            throw new IllegalArgumentException("Cloudlet scheduler cannot be null");
        }

        Vm vm = new VmSimple(mips, numberOfPes);
        vm.setRam(ram)
          .setBw(bw)
          .setSize(storage)
          .setCloudletScheduler(cloudletScheduler);

        logger.info("VM {} created with custom scheduler: {} PEs @ {} MIPS",
                    vm.getId(), numberOfPes, mips);
        return vm;
    }

    /**
     * Creates a list of VMs with default specifications.
     *
     * @param count Number of VMs to create
     * @return List of configured VM instances
     * @throws IllegalArgumentException if count is non-positive
     */
    public List<Vm> createVms(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("VM count must be positive");
        }

        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vms.add(createVm());
        }

        logger.info("Created {} VMs with default specifications", count);
        return vms;
    }

    /**
     * Creates a list of VMs with specified resource specifications.
     *
     * @param count Number of VMs to create
     * @param numberOfPes Number of PEs per VM
     * @param mips MIPS capacity per VM
     * @param ram RAM capacity in MB
     * @param storage Storage capacity in MB
     * @param bw Bandwidth capacity in Mbps
     * @return List of configured VM instances
     * @throws IllegalArgumentException if count is non-positive
     */
    public List<Vm> createVms(int count, int numberOfPes, long mips, long ram, long storage, long bw) {
        if (count <= 0) {
            throw new IllegalArgumentException("VM count must be positive");
        }

        List<Vm> vms = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            vms.add(createVm(numberOfPes, mips, ram, storage, bw));
        }

        logger.info("Created {} VMs with custom specs: {} PEs @ {} MIPS each",
                    count, numberOfPes, mips);
        return vms;
    }
}
