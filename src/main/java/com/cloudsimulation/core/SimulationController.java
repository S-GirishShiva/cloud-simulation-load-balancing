package com.cloudsimulation.core;

import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SimulationController orchestrates complete simulation workflow from infrastructure
 * creation through execution and completion.
 *
 * Provides high-level interface for running end-to-end simulations.
 */
public class SimulationController {
    private static final Logger logger = LoggerFactory.getLogger(SimulationController.class);

    private CloudSimIntegration cloudSimIntegration;
    private FederationBuilder federationBuilder;
    private CloudSimPlus simulation;

    /**
     * Creates a new SimulationController instance.
     */
    public SimulationController() {
        logger.info("SimulationController instance created");
    }

    /**
     * Runs a complete simulation workflow with specified configuration.
     *
     * @param datacenters Number of datacenters to create
     * @param hostsPerDatacenter Number of hosts per datacenter
     * @param vmsPerHost Number of VMs per host
     * @param cloudletCount Number of cloudlets to submit
     * @param simulationDuration Maximum simulation duration in seconds
     */
    public void run(int datacenters, int hostsPerDatacenter, int vmsPerHost,
                    int cloudletCount, double simulationDuration) {
        logger.info("Starting simulation: {} DCs, {} hosts/DC, {} VMs/host, {} cloudlets, {} sec duration",
                    datacenters, hostsPerDatacenter, vmsPerHost, cloudletCount, simulationDuration);

        try {
            // Step 1: Build infrastructure using FederationBuilder
            logger.info("Step 1: Building infrastructure");
            simulation = new CloudSimPlus();
            federationBuilder = new FederationBuilder(simulation);

            Federation federation = federationBuilder
                    .withDatacenters(datacenters)
                    .withHostsPerDatacenter(hostsPerDatacenter)
                    .withPesPerHost(4) // Default 4 PEs per host
                    .build();

            // Step 2: Initialize CloudSimIntegration
            logger.info("Step 2: Initializing simulation engine");
            cloudSimIntegration = new CloudSimIntegration();
            cloudSimIntegration.initialize(simulation, Collections.singletonList(federation));

            // Step 3: Create and submit VMs
            logger.info("Step 3: Creating and submitting VMs");
            int totalVms = datacenters * hostsPerDatacenter * vmsPerHost;
            List<Vm> vms = federationBuilder.getVmFactory().createVms(totalVms);
            cloudSimIntegration.submitVms(vms);

            // Step 4: Create and submit cloudlets
            logger.info("Step 4: Creating and submitting cloudlets");
            List<Cloudlet> cloudlets = createTestCloudlets(cloudletCount);
            cloudSimIntegration.submitCloudlets(cloudlets);

            // Step 5: Run simulation to completion
            logger.info("Step 5: Running simulation");
            cloudSimIntegration.start();

            while (!cloudSimIntegration.isFinished() && cloudSimIntegration.clock() < simulationDuration) {
                cloudSimIntegration.runFor(1.0);
            }

            // Step 6: Validate results
            logger.info("Step 6: Validating results");
            validateResults(vms, cloudlets);

            logger.info("Simulation completed successfully at time {}", cloudSimIntegration.clock());

        } catch (Exception e) {
            logger.error("Simulation failed with error", e);
            throw new RuntimeException("Simulation execution failed", e);
        } finally {
            // Ensure resource cleanup
            shutdown();
        }
    }

    /**
     * Creates simple test cloudlets for validation.
     *
     * @param count Number of cloudlets to create
     * @return List of test cloudlets
     */
    private List<Cloudlet> createTestCloudlets(int count) {
        List<Cloudlet> cloudlets = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            CloudletSimple cloudlet = new CloudletSimple(
                    i,          // ID
                    10000,      // Length in MI (10,000 million instructions)
                    1           // Number of PEs required
            );

            // Set utilization models
            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.1));

            cloudlets.add(cloudlet);
        }

        logger.debug("Created {} test cloudlets", count);
        return cloudlets;
    }

    /**
     * Validates simulation results.
     *
     * @param vms List of VMs that were submitted
     * @param cloudlets List of cloudlets that were executed
     */
    private void validateResults(List<Vm> vms, List<Cloudlet> cloudlets) {
        // Validate all VMs were assigned to hosts
        long assignedVms = vms.stream()
                .filter(vm -> vm.getHost() != null)
                .count();

        logger.info("VM Assignment: {}/{} VMs assigned to hosts", assignedVms, vms.size());

        if (assignedVms < vms.size()) {
            logger.warn("{} VMs were not assigned to hosts", vms.size() - assignedVms);
        }

        // Validate all cloudlets completed
        long completedCloudlets = cloudlets.stream()
                .filter(Cloudlet::isFinished)
                .count();

        logger.info("Cloudlet Completion: {}/{} cloudlets completed", completedCloudlets, cloudlets.size());

        if (completedCloudlets < cloudlets.size()) {
            logger.warn("{} cloudlets did not complete", cloudlets.size() - completedCloudlets);
        }

        // Overall validation
        boolean success = (assignedVms == vms.size()) && (completedCloudlets == cloudlets.size());
        if (success) {
            logger.info("VALIDATION PASSED: All VMs assigned and all cloudlets completed");
        } else {
            logger.error("VALIDATION FAILED: Some VMs or cloudlets not processed correctly");
        }
    }

    /**
     * Shuts down the simulation controller and cleans up resources.
     */
    public void shutdown() {
        if (cloudSimIntegration != null) {
            logger.info("Shutting down simulation controller");
            cloudSimIntegration.terminate();
        }
    }

    /**
     * Gets the CloudSimIntegration instance.
     *
     * @return CloudSimIntegration instance, or null if not initialized
     */
    public CloudSimIntegration getCloudSimIntegration() {
        return cloudSimIntegration;
    }

    /**
     * Gets the FederationBuilder instance.
     *
     * @return FederationBuilder instance, or null if not initialized
     */
    public FederationBuilder getFederationBuilder() {
        return federationBuilder;
    }
}
