package com.cloudsimulation.core;

import com.cloudsimulation.infrastructure.Federation;
import com.cloudsimulation.infrastructure.FederationBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.utilizationmodels.UtilizationModelFull;
import org.cloudsimplus.utilizationmodels.UtilizationModelDynamic;
import org.cloudsimplus.vms.Vm;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Smoke test for simulation engine integration.
 * Validates end-to-end simulation execution with minimal federation setup.
 *
 * Test must complete in less than 10 real seconds for 100 simulated seconds.
 */
class SimulationEngineSmokeTest {

    @Test
    void testCompleteSimulationFlow() {
        long startTime = System.currentTimeMillis();

        // Create minimal federation: 1 federation, 2 datacenters, 3 hosts each
        CloudSimPlus simulation = new CloudSimPlus();
        FederationBuilder builder = new FederationBuilder(simulation);

        Federation federation = builder
                .withDatacenters(2)
                .withHostsPerDatacenter(3)
                .withPesPerHost(4)
                .build();

        assertNotNull(federation, "Federation should be created");
        assertEquals(2, federation.getDatacenterCount(), "Should have 2 datacenters");

        // Initialize simulation
        CloudSimIntegration cloudSim = new CloudSimIntegration();
        cloudSim.initialize(simulation, Collections.singletonList(federation));

        assertNotNull(cloudSim.getSimulation(), "Simulation should be initialized");
        assertNotNull(cloudSim.getBroker(), "Broker should be created");

        // Create and submit VMs: 12 VMs (2 per host)
        List<Vm> vms = builder.getVmFactory().createVms(12);
        assertEquals(12, vms.size(), "Should create 12 VMs");

        cloudSim.submitVms(vms);

        // Create and submit cloudlets: 20 simple cloudlets
        List<Cloudlet> cloudlets = createTestCloudlets(20);
        assertEquals(20, cloudlets.size(), "Should create 20 cloudlets");

        cloudSim.submitCloudlets(cloudlets);

        // Run simulation
        cloudSim.start();
        assertEquals("RUNNING", cloudSim.getState(), "Simulation should be running");

        // Run for 100 simulated seconds
        while (!cloudSim.isFinished() && cloudSim.clock() < 100.0) {
            cloudSim.runFor(1.0);
        }

        // Validations
        assertTrue(cloudSim.clock() >= 0, "Simulation clock should advance");

        // Verify all VMs assigned to hosts
        long assignedVms = vms.stream()
                .filter(vm -> vm.getHost() != null)
                .count();
        assertEquals(12, assignedVms, "All VMs should be assigned to hosts");

        // Verify all cloudlets completed
        long completedCloudlets = cloudlets.stream()
                .filter(Cloudlet::isFinished)
                .count();
        assertEquals(20, completedCloudlets, "All cloudlets should complete");

        // Cleanup
        cloudSim.terminate();

        // Verify execution time
        long executionTime = System.currentTimeMillis() - startTime;
        assertTrue(executionTime < 10000,
                "Test should complete in less than 10 seconds (actual: " + executionTime + "ms)");

        System.out.println("SIMULATION ENGINE SMOKE TEST PASSED");
        System.out.println("Execution time: " + executionTime + "ms");
        System.out.println("Simulation time: " + cloudSim.clock() + " seconds");
    }

    /**
     * Creates simple test cloudlets.
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

            cloudlet.setUtilizationModelCpu(new UtilizationModelFull());
            cloudlet.setUtilizationModelRam(new UtilizationModelDynamic(0.5));
            cloudlet.setUtilizationModelBw(new UtilizationModelDynamic(0.1));

            cloudlets.add(cloudlet);
        }

        return cloudlets;
    }
}
