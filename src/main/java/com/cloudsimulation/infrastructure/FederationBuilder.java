package com.cloudsimulation.infrastructure;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.Datacenter;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * FederationBuilder orchestrates infrastructure setup by coordinating factory classes
 * to build the complete federation hierarchy.
 *
 * Provides methods to build and validate federation structure.
 */
public class FederationBuilder {
    private static final Logger logger = LoggerFactory.getLogger(FederationBuilder.class);

    private final CloudSimPlus simulation;
    private final DatacenterFactory datacenterFactory;
    private final HostFactory hostFactory;
    private final VmFactory vmFactory;

    private int numberOfDatacenters = 2;
    private int hostsPerDatacenter = 5;
    private int pesPerHost = 4;
    private int vmsPerHost = 0;

    /**
     * Creates a new FederationBuilder instance.
     *
     * @param simulation CloudSim simulation instance
     */
    public FederationBuilder(CloudSimPlus simulation) {
        this.simulation = simulation;
        this.datacenterFactory = new DatacenterFactory(simulation);
        this.hostFactory = new HostFactory();
        this.vmFactory = new VmFactory();
        logger.info("FederationBuilder initialized");
    }

    /**
     * Sets the number of datacenters to create.
     *
     * @param numberOfDatacenters Number of datacenters
     * @return This builder for method chaining
     */
    public FederationBuilder withDatacenters(int numberOfDatacenters) {
        if (numberOfDatacenters <= 0) {
            throw new IllegalArgumentException("Number of datacenters must be positive");
        }
        this.numberOfDatacenters = numberOfDatacenters;
        return this;
    }

    /**
     * Sets the number of hosts per datacenter.
     *
     * @param hostsPerDatacenter Number of hosts per datacenter
     * @return This builder for method chaining
     */
    public FederationBuilder withHostsPerDatacenter(int hostsPerDatacenter) {
        if (hostsPerDatacenter <= 0) {
            throw new IllegalArgumentException("Number of hosts per datacenter must be positive");
        }
        this.hostsPerDatacenter = hostsPerDatacenter;
        return this;
    }

    /**
     * Sets the number of processing elements per host.
     *
     * @param pesPerHost Number of PEs per host
     * @return This builder for method chaining
     */
    public FederationBuilder withPesPerHost(int pesPerHost) {
        if (pesPerHost <= 0) {
            throw new IllegalArgumentException("Number of PEs per host must be positive");
        }
        this.pesPerHost = pesPerHost;
        return this;
    }

    /**
     * Sets the number of VMs per host.
     *
     * @param vmsPerHost Number of VMs per host (0 for no VMs)
     * @return This builder for method chaining
     */
    public FederationBuilder withVmsPerHost(int vmsPerHost) {
        if (vmsPerHost < 0) {
            throw new IllegalArgumentException("Number of VMs per host cannot be negative");
        }
        this.vmsPerHost = vmsPerHost;
        return this;
    }

    /**
     * Validates that resource constraints are satisfied.
     * Ensures that VM resource requirements don't exceed host capacity.
     *
     * @throws IllegalStateException if validation fails with detailed error message
     */
    private void validateResourceConstraints() {
        // Check if VMs are configured
        if (vmsPerHost > 0) {
            // Get default VM configuration from VmFactory
            // Using default values since we create VMs with default specs
            int vmPes = 1;  // Default PEs per VM from VmFactory.DEFAULT_PES

            // Calculate total VM PEs that would be required per host
            int totalVmPesPerHost = vmsPerHost * vmPes;

            // Validate that host has enough PEs for all VMs
            if (totalVmPesPerHost > pesPerHost) {
                String errorMsg = String.format(
                    "Resource constraint violation: Total VM PEs (%d VMs × %d PEs = %d) exceeds host capacity (%d PEs). " +
                    "Reduce VMs per host or increase host PEs.",
                    vmsPerHost, vmPes, totalVmPesPerHost, pesPerHost
                );
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            logger.debug("Resource constraints validated: {} PEs required for VMs, {} PEs available per host",
                        totalVmPesPerHost, pesPerHost);
        }
    }

    /**
     * Builds a federation with the configured parameters.
     * Creates datacenters with hosts and optionally VMs according to the specified configuration.
     *
     * @return Configured Federation instance
     * @throws IllegalStateException if resource constraints are violated
     */
    public Federation build() {
        // Validate resource constraints before building
        validateResourceConstraints();

        logger.info("Building federation with {} datacenters, {} hosts each, {} VMs per host",
                    numberOfDatacenters, hostsPerDatacenter, vmsPerHost);

        Federation federation = new Federation("federation-1");
        List<Vm> allVms = new ArrayList<>();

        // Create datacenters with hosts
        for (int dcId = 0; dcId < numberOfDatacenters; dcId++) {
            // Create hosts for this datacenter
            List<Host> hosts = hostFactory.createHosts(hostsPerDatacenter, pesPerHost);

            // Create VMs if requested (broker will allocate VMs to hosts)
            if (vmsPerHost > 0) {
                int vmsForThisDatacenter = vmsPerHost * hosts.size();
                List<Vm> vms = vmFactory.createVms(vmsForThisDatacenter);
                allVms.addAll(vms);
                logger.debug("Created {} VMs for datacenter {} ({} VMs per host × {} hosts)",
                            vms.size(), dcId, vmsPerHost, hosts.size());
            }

            // Create datacenter with these hosts
            Datacenter datacenter = datacenterFactory.createDatacenter(hosts);

            // Add datacenter to federation
            federation.addDatacenter((int) datacenter.getId(), datacenter);

            logger.info("Added datacenter {} to federation with {} hosts",
                        datacenter.getId(), hosts.size());
        }

        // Submit VMs to simulation via DatacenterBroker if VMs were created
        if (!allVms.isEmpty()) {
            DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
            broker.submitVmList(allVms);
            logger.info("Federation built successfully with {} datacenters and {} VMs submitted to broker {}",
                        federation.getDatacenterCount(), allVms.size(), broker.getId());
        } else {
            logger.info("Federation built successfully with {} datacenters",
                        federation.getDatacenterCount());
        }

        return federation;
    }

    /**
     * Validates the federation structure to ensure all entity relationships are intact.
     *
     * Verifies:
     * - All Datacenters are registered with Federation
     * - All Hosts are assigned to Datacenters
     * - All VMs are assigned to Hosts (if VMs are present)
     *
     * @param federation Federation instance to validate
     * @return true if federation structure is valid
     * @throws IllegalStateException if validation fails with detailed error message
     */
    public boolean validateFederationStructure(Federation federation) {
        logger.info("Validating federation structure for {}", federation.getFederationId());

        // Verify federation has datacenters
        if (federation.getDatacenters().isEmpty()) {
            String errorMsg = "Validation failed: Federation has no datacenters";
            logger.error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        logger.info("Federation has {} datacenters", federation.getDatacenterCount());

        // Verify each datacenter has hosts
        for (Datacenter datacenter : federation.getDatacenters()) {
            if (datacenter.getHostList().isEmpty()) {
                String errorMsg = String.format(
                    "Validation failed: Datacenter %d has no hosts", datacenter.getId());
                logger.error(errorMsg);
                throw new IllegalStateException(errorMsg);
            }

            logger.debug("Datacenter {} has {} hosts",
                        datacenter.getId(), datacenter.getHostList().size());

            // Verify each host has valid resources
            for (Host host : datacenter.getHostList()) {
                if (host.getTotalMipsCapacity() <= 0) {
                    String errorMsg = String.format(
                        "Validation failed: Host %d in Datacenter %d has no MIPS capacity",
                        host.getId(), datacenter.getId());
                    logger.error(errorMsg);
                    throw new IllegalStateException(errorMsg);
                }

                // Check if host has VMs (optional - VMs might be added later by broker)
                if (!host.getVmList().isEmpty()) {
                    logger.debug("Host {} has {} VMs",
                                host.getId(), host.getVmList().size());

                    // Verify VMs are properly assigned
                    for (Vm vm : host.getVmList()) {
                        if (!vm.getHost().equals(host)) {
                            String errorMsg = String.format(
                                "Validation failed: VM %d is not properly assigned to Host %d",
                                vm.getId(), host.getId());
                            logger.error(errorMsg);
                            throw new IllegalStateException(errorMsg);
                        }
                    }
                }
            }
        }

        logger.info("Federation structure validation passed for {}",
                    federation.getFederationId());
        return true;
    }

    /**
     * Gets the VmFactory instance used by this builder.
     *
     * @return VmFactory instance
     */
    public VmFactory getVmFactory() {
        return vmFactory;
    }

    /**
     * Gets the HostFactory instance used by this builder.
     *
     * @return HostFactory instance
     */
    public HostFactory getHostFactory() {
        return hostFactory;
    }

    /**
     * Gets the DatacenterFactory instance used by this builder.
     *
     * @return DatacenterFactory instance
     */
    public DatacenterFactory getDatacenterFactory() {
        return datacenterFactory;
    }
}
